/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */

#define ACME   1
#define MODULE 2
void emit_flags(int flags);
void emit_header(void);
void emit_trailer(void);
void emit_moddep(char *name, int len);
void emit_sysflags(int val);
void emit_bytecode_seg(void);
void emit_comment(char *s);
void emit_asm(char *s);
void emit_idlocal(char *name, int value);
void emit_idglobal(int value, int size, char *name);
void emit_idfunc(int tag, int type, char *name);
void emit_idconst(char *name, int value);
void emit_def(char *name, int is_bytecode);
int emit_data(int vartype, int consttype, long constval, int constsize);
void emit_codetag(int tag);
void emit_const(int cval);
void emit_conststr(long conststr, int strsize);
void emit_lb(void);
void emit_lw(void);
void emit_llb(int index);
void emit_llw(int index);
void emit_lab(int tag, int offset, int type);
void emit_law(int tag, int offset, int type);
void emit_sb(void);
void emit_sw(void);
void emit_slb(int index);
void emit_slw(int index);
void emit_dlb(int index);
void emit_dlw(int index);
void emit_sab(int tag, int offset, int type);
void emit_saw(int tag, int ofset, int type);
void emit_dab(int tag, int type);
void emit_daw(int tag, int type);
void emit_call(int tag, int type);
void emit_ical(void);
void emit_localaddr(int index);
void emit_globaladdr(int tag, int offset, int type);
void emit_indexbyte(void);
void emit_indexword(void);
int emit_unaryop(int op);
int emit_op(t_token op);
void emit_brtru(int tag);
void emit_brfls(int tag);
void emit_brgt(int tag);
void emit_brlt(int tag);
void emit_brne(int tag);
void emit_brnch(int tag);
void emit_swap(void);
void emit_dup(void);
void emit_push(void);
void emit_pull(void);
void emit_drop(void);
void emit_leave(void);
void emit_ret(void);
void emit_enter(int cparams);
void emit_start(void);
void emit_rld(void);
void emit_esd(void);
