#include <stdio.h>
#include "tokens.h"
#include "lex.h"
#include "codegen.h"
#include "parse.h"

int main(int argc, char **argv)
{

        if (argc > 1 && argv[1][0] == '-' && argv[1][1] == 'A')
            emit_flags(ACME);
        if (parse_module())
        {
            fprintf(stderr, "Compilation complete.\n");
        }
        return (0);
}
