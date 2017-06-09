/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
/*
 * Global flags.
 */
#define ACME            (1<<0)
#define MODULE          (1<<1)
#define OPTIMIZE        (1<<2)
#define BYTECODE_SEG    (1<<3)
#define INIT            (1<<4)
#define SYSFLAGS        (1<<5)
#define WARNINGS        (1<<6)
extern int outflags;
#include "tokens.h"
#include "lex.h"
#include "symbols.h"
#include "parse.h"
#include "codegen.h"