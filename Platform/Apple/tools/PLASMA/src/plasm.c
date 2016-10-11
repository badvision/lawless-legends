/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */

#include <stdio.h>
#include "tokens.h"
#include "lex.h"
#include "codegen.h"
#include "parse.h"

int main(int argc, char **argv)
{
    int j, i, flags = 0;
    for (i = 1; i < argc; i++)
    {
        if (argv[i][0] == '-')
        { 
            j = 1;
            while (argv[i][j])
            {
                switch(argv[i][j++])
                {
                    case 'A':
                        flags |= ACME;
                        break;
                    case 'M':
                        flags |= MODULE;
                        break;
                }
            }
        }
    }
    emit_flags(flags);
    if (parse_module())
    {
        fprintf(stderr, "Compilation complete.\n");
    }
    return (0);
}
