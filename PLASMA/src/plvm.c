#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>

typedef unsigned char  code;
typedef unsigned char  byte;
typedef signed   short word;
typedef unsigned short uword;
typedef unsigned short address;
/*
 * Debug
 */
int show_state = 0;
/*
 * Bytecode memory
 */
#define BYTE_PTR(bp)	((byte)(*bp++))
#define WORD_PTR(bp)	((word)(*bp++|(*++bp << 8)))
#define UWORD_PTR(bp)	((uword)(*bp++|(*++bp << 8)))
#define MOD_ADDR	0x1000
#define DEF_CALL	0x0800
#define DEF_CALLSZ	0x0800
#define DEF_ENTRYSZ	6
#define MEM_SIZE	65536
byte mem_data[MEM_SIZE], mem_code[MEM_SIZE];
byte *mem_bank[2] = {mem_data, mem_code};
uword sp = 0x01FE, fp = 0xBEFF, heap = 0x6000, xheap = 0x0800, deftbl = DEF_CALL, lastdef = DEF_CALL;

#define EVAL_STACKSZ	16
#define PUSH(v)	(*(--esp))=(v)
#define POP		(*(esp++))
#define UPOP		((uword)(*(esp++)))
#define TOS		(esp[0])
word eval_stack[EVAL_STACKSZ];
word *esp = eval_stack + EVAL_STACKSZ;

#define SYMTBLSZ	1024
#define SYMSZ		16
#define MODTBLSZ	128
#define MODSZ		16
#define MODLSTSZ	32
byte symtbl[SYMTBLSZ];
byte *lastsym = symtbl;
byte modtbl[MODTBLSZ];
byte *lastmod = modtbl;
/*
 * Utility routines.
 * 
 * A DCI string is one that has the high bit set for every character except the last.
 * More efficient than C or Pascal strings.
 */
int dcitos(byte *dci, char *str)
{
    int len = 0;
    do
        str[len] = *dci & 0x7F;
    while ((len++ < 15) && (*dci++ & 0x80));
    str[len] = 0;
    return len;
}
int stodci(char *str, byte *dci)
{
    int len = 0;
    do
        dci[len] = toupper(*str) | 0x80;
    while (*str++ && (len++ < 15));
    dci[len - 1] &= 0x7F;
    return len;
}

/*
 * Heap routines.
 */
uword avail_heap(void)
{
    return fp - heap;
}
uword alloc_heap(int size)
{
    uword addr = heap;
    heap += size;
    if (heap >= fp)
    {
        printf("Error: heap/frame collision.\n");
        exit (1);
    }
    return addr;
}
uword free_heap(int size)
{
    heap -= size;
    return fp - heap;
}
uword mark_heap(void)
{
    return heap;
}
int release_heap(uword newheap)
{
    heap = newheap;
    return fp - heap;
}
uword avail_xheap(void)
{
    return 0xC000 - xheap;
}
uword alloc_xheap(int size)
{
    uword addr = xheap;
    xheap += size;
    if (xheap >= 0xC000)
    {
        printf("Error: xheap extinguished.\n");
        exit (1);
    }
    return addr;
}
uword free_xheap(int size)
{
    xheap -= size;
    return 0xC000 - heap;
}
uword mark_xheap(void)
{
    return xheap;
}
int release_xheap(uword newxheap)
{
    xheap = newxheap;
    return 0xC000 - xheap;
}
/*
 * Copy from data mem to code mem.
 */
void xmemcpy(uword src, uword dst, uword size)
{
    while (size--)
        mem_code[dst + size] = mem_data[src + size]; 
}
/*
 * Copy from code mem to data mem.
 */
void memxcpy(uword src, uword dst, uword size)
{
    while (size--)
        mem_data[dst + size] = mem_code[src + size]; 
}
/*
 * DCI table routines,
 */
void dump_tbl(byte *tbl)
{
    int len;
    byte *entbl;
    while (*tbl)
    {
        len = 0;
        while (*tbl & 0x80)
        {
            putchar(*tbl++ & 0x7F);
            len++;
        }
        putchar(*tbl++);
        putchar(':');
        while (len++ < 15)
            putchar(' ');
        printf("$%04X\n", tbl[0] | (tbl[1] << 8)); 
        tbl += 2;
    }
}
uword lookup_tbl(byte *dci, byte *tbl)
{
    char str[20];
    byte *match, *entry = tbl;
    while (*entry)
    {
        match = dci;
        while (*entry == *match)
        {
            if ((*entry & 0x80) == 0)
                return entry[1] | (entry[2] << 8);
            entry++;
            match++;
        }
        while (*entry++ & 0x80);
        entry += 2;
    }
    dcitos(dci, str);
    return 0;
}
int add_tbl(byte *dci, int val, byte *tbl, byte **last)
{
    while (*dci & 0x80)
        *(*last)++ = *dci++;
    *(*last)++ = *dci++;
    *(*last)++ = val;
    *(*last)++ = val >> 8;
}

/*
 * Symbol table routines.
 */
void dump_sym(void)
{
    printf("\nSystem Symbol Table:\n");
    dump_tbl(symtbl);
}
uword lookup_sym(byte *sym)
{
    return lookup_tbl(sym, symtbl);
}
int add_sym(byte *sym, int addr)
{
    return add_tbl(sym, addr, symtbl, &lastsym);
}

/*
 * Module routines.
 */
void dump_mod(void)
{
    printf("\nSystem Module Table:\n");
    dump_tbl(modtbl);
}
uword lookup_mod(byte *mod)
{
    return lookup_tbl(mod, modtbl);
}
int add_mod(byte *mod, int addr)
{
    return add_tbl(mod, addr, symtbl, &lastmod);
}
defcall_add(int bank, int addr)
{
    mem_data[lastdef]     = bank ? 2 : 1;
    mem_data[lastdef + 1] = addr;
    mem_data[lastdef + 2] = addr >> 8;
    return lastdef++;
}
int def_lookup(byte *cdd, int defaddr)
{
    int i, calldef = 0;
    for (i = 0; cdd[i * 4] == 0x02; i++)
    {
        if ((cdd[i * 4 + 1] | (cdd[i * 4 + 2] << 8)) == defaddr)
        {
            calldef = cdd + i * 4 - mem_data;
            break;
        }
    }
    return calldef;
}
int extern_lookup(byte *esd, int index)
{
    byte *sym;
    char string[32];
    while (*esd)
    {
        sym = esd;
        esd += dcitos(esd, string);
        if ((esd[0] & 0x10) && (esd[1] == index))
            return lookup_sym(sym);
        esd += 3;
    }
    printf("\nError: extern index %d not found in ESD.\n", index);
    return 0;
}
int load_mod(byte *mod)
{
    int len, size, end, magic, bytecode, fixup, addr, modaddr = mark_heap();
    byte *moddep, *rld, *esd, *cdd, *sym;
    byte header[128];
    char filename[32], string[17];

    dcitos(mod, filename);
    printf("Load module %s\n");
    int fd = open(filename, O_RDONLY, 0);
    if ((fd > 0) && (len = read(fd, header, 128)) > 0)
    {
        magic = header[2] | (header[3] << 8);
        if (magic == 0xDA7E)
        {
            /*
             * This is a relocatable bytecode module.
             */
            bytecode = header[4] | (header[5] << 8);
            moddep   = header + 6;
            if (*moddep)
            {
                /*
                 * Load module dependencies.
                 */
                close(fd);
                while (*moddep)
                {
                    if (lookup_mod(moddep) == 0)
                        load_mod(moddep);
                    moddep += dcitos(moddep, string);
                }
                modaddr = mark_heap();
                fd = open(filename, O_RDONLY, 0);
                len = read(fd, mem_data + modaddr, 128);
            }
            else
                memcpy(mem_data + modaddr, header, len);
        }
        addr = modaddr + len;
        while ((len = read(fd, mem_data + addr, 4096)) > 0)
            addr += len;
        close(fd);
        size    = addr - modaddr;
        len     = mem_data[modaddr + 0] | (mem_data[modaddr + 1] << 8);
        end     = modaddr + len;
        rld     = mem_data + modaddr + len; // Re-Locatable Directory
        esd     = rld; // Extern+Entry Symbol Directory
        bytecode += modaddr - MOD_ADDR;
        while (*esd != 0x00) // Scan to end of RLD
            esd += 4;
        esd++;
        cdd = rld;
        if (show_state)
        {
            /*
             * Dump different parts of module.
             */
            printf("Module size: %d\n", size);
            printf("Module code+data size: %d\n", len);
            printf("Module magic: $%04X\n", magic);
            printf("Module bytecode: $%04X\n", bytecode);
        }
        /*
         * Print out the Re-Location Dictionary.
         */
        if (show_state)
            printf("\nRe-Location Dictionary:\n");
        while (*rld)
        {
            if (rld[0] == 0x02)
            {
                if (show_state) printf("\tDEF         CODE");
                addr = rld[1] | (rld[2] << 8);
                addr += modaddr - MOD_ADDR;
                rld[1] = addr;
                rld[2] = addr >> 8;
                end = rld - mem_data + 4;
            }
            else
            {
                addr = rld[1] | (rld[2] << 8);
                addr += modaddr - MOD_ADDR;
                if (rld[0] & 0x80)
                    fixup = mem_data[addr] | (mem_data[addr + 1] << 8);
                else
                    fixup = mem_data[addr];
                if (rld[0] & 0x10)
                {
                    if (show_state) printf("\tEXTERN[$%02X] ", rld[3]);
                    fixup += extern_lookup(esd, rld[3]);
                }
                else
                {
                    if (show_state) printf("\tINTERN      ");
                    fixup += modaddr - MOD_ADDR;
                    if (fixup >= bytecode)
                        /*
                         * Replace with call def dictionary.
                         */
                        fixup = def_lookup(cdd, fixup);
                }
                if (rld[0] & 0x80)
                {
                    if (show_state) printf("WORD");
                    mem_data[addr]     = fixup;
                    mem_data[addr + 1] = fixup >> 8;
                }
                else
                {
                    if (show_state) printf("BYTE");
                    mem_data[addr] = fixup;
                }
                
            }
            if (show_state) printf("@$%04X\n", addr);
            rld += 4;
        }
        if (show_state) printf("\nExternal/Entry Symbol Directory:\n");
        while (*esd)
        {
            sym = esd;
            esd += dcitos(esd, string);
            if (esd[0] & 0x10)
            {
                if (show_state) printf("\tIMPORT %s[$%02X]\n", string, esd[1]);
            }
            else if (esd[0] & 0x08)
            {
                addr = esd[1] | (esd[2] << 8);
                addr += modaddr - MOD_ADDR;
                if (show_state) printf("\tEXPORT %s@$%04X\n", string, addr);
                if (addr >= bytecode)
                    addr = def_lookup(cdd, addr);
                add_sym(sym, addr);
            }
            esd += 3;
        }
    }
    else
    {
        printf("Error: Unable to load module %s\n", filename);
        exit (1);
    }
    /*
     * Reserve heap space for relocated module.
     */
    alloc_heap(end - modaddr);
    return (fd > 0);
}
void interp(code *ip);

void call(word pc)
{
    int i, s;
    char sz[64];

    switch (mem_data[pc++])
    {
        case 0: // NULL call
            printf("NULL call code\n");
            break;
        case 1: // BYTECODE in mem_code
            interp(mem_code + (mem_data[pc] + (mem_data[pc + 1] << 8)));
            break;
        case 2: // BYTECODE in mem_data
            interp(mem_data + (mem_data[pc] + (mem_data[pc + 1] << 8)));
            break;
        case 3: // LIBRARY STDLIB::VIEWPORT
            printf("Set Window %d, %d, %d, %n/n", POP, POP, POP, POP);
            PUSH(0);
            break;
        case 4: // LIBRARY STDLIB::PUTC
            putchar(POP);
            PUSH(0);
            break;
        case 5: // LIBRARY STDLIB::PUTS
            s = POP;
            i = mem_data[s++];
            PUSH(i);
            while (i--)
                putchar(mem_data[s++]);
            break;
        case 6: // LIBRARY STDLIB::PUTSZ
            s = POP;
            while (i = mem_data[s++])
            {
                if (i == '\r')
                    i = '\n';
                putchar(i);
            }
            PUSH(0);
            break;
        case 7: // LIBRARY STDLIB::GETC
            PUSH(getchar());
            break;
        case 8: // LIBRARY STDLIB::GETS
            gets(sz);
            i = 0;
            while (sz[i])
                mem_data[0x200 + i++] = sz[i];
            mem_data[0x200 + i] = 0;
            mem_data[0x1FF] = i;
            PUSH(i);
            break;
        case 9: // LIBRARY STDLIB::CLS
            puts("\033[2J");
            fflush(stdout);
            PUSH(0);
            PUSH(0);
        case 10: // LIBRARY STDLIB::GOTOXY
            s = POP + 1;
            i = POP + 1;
            printf("\033[%d;%df", s, i);
            fflush(stdout);
            PUSH(0);
            break;
        default:
            printf("Bad call code\n");
    }
}
    
/*
 * OPCODE TABLE
 *
OPTBL:	DW	ZERO,ADD,SUB,MUL,DIV,MOD,INCR,DECR		; 00 02 04 06 08 0A 0C 0E
       	DW	NEG,COMP,AND,IOR,XOR,SHL,SHR,IDXW		; 10 12 14 16 18 1A 1C 1E
       	DW	NOT,LOR,LAND,LA,LLA,CB,CW,SWAP			; 20 22 24 26 28 2A 2C 2E
       	DW	DROP,DUP,PUSH,PULL,BRGT,BRLT,BREQ,BRNE		; 30 32 34 36 38 3A 3C 3E
       	DW	ISEQ,ISNE,ISGT,ISLT,ISGE,ISLE,BRFLS,BRTRU	; 40 42 44 46 48 4A 4C 4E
       	DW	BRNCH,IBRNCH,CALL,ICAL,ENTER,LEAVE,RET,??? 	; 50 52 54 56 58 5A 5C 5E
       	DW	LB,LW,LLB,LLW,LAB,LAW,DLB,DLW			; 60 62 64 66 68 6A 6C 6E
       	DW	SB,SW,SLB,SLW,SAB,SAW,DAB,DAW			; 70 72 74 76 78 7A 7C 7E
*/
void interp(code *ip)
{
    word val, ea, frmsz, parmcnt;

    while (1)
    {
        if (show_state)
        {
            word *dsp = &eval_stack[EVAL_STACKSZ - 1];
            printf("$%04X: $%02X [ ", ip - mem_data, *ip);
            while (dsp >= esp)
                printf("$%04X ", (*dsp--) & 0xFFFF);
            printf("]\n");
        }
        switch (*ip++)
        {
	    /*
	     * 0x00-0x0F
	     */
            case 0x00: // ZERO : TOS = 0
                PUSH(0);
                break;
            case 0x02: // ADD : TOS = TOS + TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea + val);
                break;
            case 0x04: // SUB : TOS = TOS-1 - TOS
                val = POP;
                ea  = POP;
                PUSH(ea - val);
                break;
            case 0x06: // MUL : TOS = TOS * TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea * val);
                break;
            case 0x08: // DIV : TOS = TOS-1 / TOS
                val = POP;
                ea  = POP;
                PUSH(ea / val);
                break;
            case 0x0A: // MOD : TOS = TOS-1 % TOS
                val = POP;
                ea  = POP;
                PUSH(ea % val);
                break;
            case 0x0C: // INCR : TOS = TOS + 1
                TOS++;
                break;
            case 0x0E: // DECR : TOS = TOS - 1
                TOS--;
                break;
                /*
                 * 0x10-0x1F
                 */
            case 0x10: // NEG : TOS = -TOS
                TOS = -TOS;
                break;
            case 0x12: // COMP : TOS = ~TOS
                TOS = ~TOS;
                break;
            case 0x14: // AND : TOS = TOS & TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea & val);
                break;
            case 0x16: // IOR : TOS = TOS ! TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea | val);
                break;
            case 0x18: // XOR : TOS = TOS ^ TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea ^ val);
                break;
            case 0x1A: // SHL : TOS = TOS-1 << TOS
                val = POP;
                ea  = POP;
                PUSH(ea << val);
                break;
            case 0x1C: // SHR : TOS = TOS-1 >> TOS
                val = POP;
                ea  = POP;
                PUSH(ea >> val);
                break;
            case 0x1E: // IDXW : TOS = TOS * 2
                TOS *= 2;
                break;
                /*
                 * 0x20-0x2F
                 */
            case 0x20: // NOT : TOS = !TOS
                TOS = !TOS;
                break;
            case 0x22: // LOR : TOS = TOS || TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea || val);
                break;
            case 0x24: // LAND : TOS = TOS && TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea && val);
                break;
            case 0x26: // LA : TOS = @VAR ; equivalent to CW ADDRESSOF(VAR)
                PUSH(WORD_PTR(ip));
                break;
            case 0x28: // LLA : TOS = @LOCALVAR ; equivalent to CW FRAMEPTR+OFFSET(LOCALVAR)
                PUSH(fp + BYTE_PTR(ip));
                break;
            case 0x2A: // CB : TOS = CONSTANTBYTE (IP)
                PUSH(BYTE_PTR(ip));
                break;
            case 0x2C: // CW : TOS = CONSTANTWORD (IP)
                PUSH(WORD_PTR(ip));
                break;
            case 0x2E: // SWAP : TOS = TOS-1, TOS-1 = TOS
                val = POP;
                ea  = POP;
                PUSH(val);
                PUSH(ea);
                break;
                /*
                 * 0x30-0x3F
                 */
            case 0x30: // DROP : TOS =
                esp++;;
                break;
            case 0x32: // DUP : TOS = TOS
                val = TOS;
                PUSH(val);
                break;
            case 0x34: // PUSH : TOSP = TOS
                val = POP;
                mem_data[sp--] = val >> 8;
                mem_data[sp--] = val;
                break;
            case 0x36: // PULL : TOS = TOSP
                PUSH(mem_data[++sp] | (mem_data[++sp] << 8));
                break;
            case 0x38: // BRGT : TOS-1 > TOS ? IP += (IP)
                val = POP;
                ea  = POP;
                if (ea <= val)
                    ip += WORD_PTR(ip) - 2;
                else
                    ip += 2;
                break;
            case 0x3A: // BRLT : TOS-1 < TOS ? IP += (IP)
                val = POP;
                ea  = TOS;
                if (ea >= val)
                    ip += WORD_PTR(ip) - 2;
                else
                    ip += 2;
                break;
            case 0x3C: // BREQ : TOS == TOS-1 ? IP += (IP)
                val = POP;
                ea  = TOS;
                if (ea == val)
                    ip += WORD_PTR(ip) - 2;
                else
                    ip += 2;
                break;
            case 0x3E: // BRNE : TOS != TOS-1 ? IP += (IP)
                val = POP;
                ea  = TOS;
                if (ea != val)
                    ip += WORD_PTR(ip) - 2;
                else
                    ip += 2;
                break;
                /*
                 * 0x40-0x4F
                 */
            case 0x40: // ISEQ : TOS = TOS == TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea == val);
                break;
            case 0x42: // ISNE : TOS = TOS != TOS-1
                val = POP;
                ea  = POP;
                PUSH(ea != val);
                break;
            case 0x44: // ISGT : TOS = TOS-1 > TOS
                val = POP;
                ea  = POP;
                PUSH(ea <= val);
                break;
            case 0x46: // ISLT : TOS = TOS-1 < TOS
                val = POP;
                ea  = POP;
                PUSH(ea >= val);
                break;
            case 0x48: // ISGE : TOS = TOS-1 >= TOS
                val = POP;
                ea  = POP;
                PUSH(ea < val);
                break;
            case 0x4A: // ISLE : TOS = TOS-1 <= TOS
                val = POP;
                ea  = POP;
                PUSH(ea > val);
                break;
            case 0x4C: // BRFLS : !TOS ? IP += (IP)
                if (!POP)
                    ip += WORD_PTR(ip) - 2;
                else
                    ip += 2;
                break;
            case 0x4E: // BRTRU : TOS ? IP += (IP)
                if (POP)
                    ip += WORD_PTR(ip) - 2;
                else
                    ip += 2;
                break;
                /*
                 * 0x50-0x5F
                 */
            case 0x50: // BRNCH : IP += (IP)
                ip += WORD_PTR(ip) - 2;
                break;
            case 0x52: // IBRNCH : IP += TOS
                ip += POP;
                break;
            case 0x54: // CALL : TOFP = IP, IP = (IP) ; call
                call(UWORD_PTR(ip));
                break;
            case 0x56: // ICALL : TOFP = IP, IP = (TOS) ; indirect call
                val = POP;
                ea = mem_data[val] | (mem_data[val + 1] << 8);
                call(ea);
                break;
            case 0x58: // ENTER : NEW FRAME, FOREACH PARAM LOCALVAR = TOS
                frmsz = BYTE_PTR(ip);
                mem_data[fp - frmsz]     = fp;
                mem_data[fp - frmsz + 1] = fp >> 8;
                fp -= frmsz;
                parmcnt = BYTE_PTR(ip);
                while (parmcnt--)
                {
                    val = POP;
                    mem_data[fp + parmcnt + 2] = val;
                    mem_data[fp + parmcnt + 3] = val >> 8;
                }
                break;
            case 0x5A: // LEAVE : DEL FRAME, IP = TOFP
                fp = mem_data[fp] | (mem_data[fp + 1] << 8);
            case 0x5C: // RET : IP = TOFP
                return;
            case 0x5E: // ???
                break;
                /*
                 * 0x60-0x6F
                 */
            case 0x60: // LB : TOS = BYTE (TOS)
                val = UPOP;
                PUSH(mem_data[val]);
                break;
            case 0x62: // LW : TOS = WORD (TOS)
                ea = POP;
                PUSH(mem_data[ea] | (mem_data[ea + 1] << 8));
                break;
            case 0x64: // LLB : TOS = LOCALBYTE [IP]
                PUSH(mem_data[fp + BYTE_PTR(ip)]);
                break;
            case 0x66: // LLW : TOS = LOCALWORD [IP]
                ea = fp + BYTE_PTR(ip);
                PUSH(mem_data[ea] | (mem_data[ea + 1] << 8));
                break;
            case 0x68: // LAB : TOS = BYTE (IP)
                PUSH(mem_data[UWORD_PTR(ip)]);
                break;
            case 0x6A: // LAW : TOS = WORD (IP)
                ea = UWORD_PTR(ip);
                PUSH(mem_data[ea] | (mem_data[ea + 1] << 8));
                break;
            case 0x6C: // DLB : TOS = TOS, LOCALBYTE [IP] = TOS
                mem_data[fp + BYTE_PTR(ip)] = TOS;
                break;
            case 0x6E: // DLW : TOS = TOS, LOCALWORD [IP] = TOS
                ea = fp + BYTE_PTR(ip);
                mem_data[ea]     = TOS;
                mem_data[ea + 1] = TOS >> 8;
                break;
                /*
                 * 0x70-0x7F
                 */
            case 0x70: // SB : BYTE (TOS) = TOS-1
                val = POP;
                ea  = POP;
                mem_data[ea] = val;
                break;
            case 0x72: // SW : WORD (TOS) = TOS-1
                val = POP;
                ea  = POP;
                mem_data[ea]     = val;
                mem_data[ea + 1] = val >> 8;
                break;
            case 0x74: // SLB : LOCALBYTE [TOS] = TOS-1
                mem_data[fp + BYTE_PTR(ip)] = POP;
                break;
            case 0x76: // SLW : LOCALWORD [TOS] = TOS-1
                ea  = fp + BYTE_PTR(ip);
                val = POP;
                mem_data[ea]     = val;
                mem_data[ea + 1] = val >> 8;
                break;
            case 0x78: // SAB : BYTE (IP) = TOS
                mem_data[WORD_PTR(ip)] = POP;
                break;
            case 0x7A: // SAW : WORD (IP) = TOS
                ea = WORD_PTR(ip);
                val = POP;
                mem_data[ea]     = val;
                mem_data[ea + 1] = val >> 8;
                break;
            case 0x7C: // DAB : TOS = TOS, BYTE (IP) = TOS
                mem_data[WORD_PTR(ip)] = TOS;
                break;
            case 0x7E: // DAW : TOS = TOS, WORD (IP) = TOS
                ea = WORD_PTR(ip);
                mem_data[ea]     = TOS;
                mem_data[ea + 1] = TOS >> 8;
                break;
                /*
                 * Odd codes and everything else are errors.
                 */
            default:
                fprintf(stderr, "Illegal opcode 0x%02X @ 0x%04X\n", ip[-1], ip - mem_code);
        }
    }
}

char *stdlib_exp[] = {
    "VIEWPORT",
    "PUTC",
    "PUTS",
    "PUTSZ",
    "GETC",
    "GETS",
    "CLS",
    "GOTOXY"
};

byte stdlib[] = {
    0x00
};

int main(int argc, char **argv)
{
    byte dci[32];
    int i;
    
    if (--argc)
    {
        argv++;
        if ((*argv)[0] == '-' && (*argv)[1] == 's')
        {
            show_state = 1;
            argc--;
            argv++;
        }
        /*
         * Add default library.
         */
        stodci("STDLIB", dci);
        add_mod(dci, 0xFFFF);
        for (i = 0; i < 8; i++)
        {
            mem_data[i] = i + 3;
            stodci(stdlib_exp[i], dci);
            add_sym(dci, i);
        }
        if (argc)
        {
            stodci(*argv, dci);
            load_mod(dci);
            if (show_state) dump_sym();
            argc--;
            argv++;
        }
        if (argc)
        {
            stodci(*argv, dci);
            call(lookup_sym(dci));
        }
    }
    return 0;
}