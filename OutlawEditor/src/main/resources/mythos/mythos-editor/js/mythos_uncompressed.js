/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
/* global Blockly, goog */

if (typeof Mythos === "undefined") {
// Hook up the rename function to notify the java editor when changes occur
    if (typeof window === "undefined") {
        window = {};
    }
    Blockly.Procedures.rename_old = Blockly.Procedures.rename;
    Blockly.Procedures.rename = function (name) {
        Mythos.editor.setFunctionName(name);
        return Blockly.Procedures.rename_old.call(this, name);
    };
    Mythos = {
        setScriptXml: function (xml) {
            Blockly.mainWorkspace.clear();
            var dom = Blockly.Xml.textToDom(xml);
            Blockly.Xml.domToWorkspace(Blockly.mainWorkspace, dom);
            while (Blockly.mainWorkspace.topBlocks_.length > 1) {
                Blockly.mainWorkspace.topBlocks_[1].dispose();
            }
        },
        getScriptXml: function () {
            return Blockly.Xml.workspaceToDom(Blockly.mainWorkspace).innerHTML;
        },
        helpUrl: 'https://docs.google.com/document/d/1VXbiY4G533-cokjQevZFhwvqMMCL--17ziMAoFoeJ5M/edit#heading=h.yv9dmneqjr2b',
        initCustomDefinitions: function () {
//            Mythos.editor.log("Add user defined types");
            Mythos.addUserDefinedTypes();
//            Mythos.editor.log("Add custom variables");
            Mythos.addCustomVariables();
//            Mythos.editor.log("Add global functions");
            Mythos.addFunctionsFromGlobalScope();
//            Mythos.editor.log("Add local functions");
            Mythos.addFunctionsFromLocalScope();
//            Mythos.editor.log("Reinitalizing toolbox");
            Mythos.workspace.updateToolbox(document.getElementById('toolbox'));
//            Mythos.editor.log("Done");
        },
        each: function (list, func) {
            if (list && list instanceof Array) {
                for (var i = 0; i < list.length; i++) {
                    func(list[i]);
                }
            } else if (list) {
                for (var i = 0; i < list.size(); i++) {
                    func(list.get(i));
                }
            }
        },
        addUserDefinedTypes: function () {
            var toolbarCategory = document.getElementById("customTypes");
            Mythos.each(Mythos.editor.getUserTypes(), function (userType) {
                var typeNode = document.createElement("block");
                typeNode.setAttribute("type", "userType_" + userType.getName());
                toolbarCategory.appendChild(typeNode);
                var getNode = document.createElement("block");
                getNode.setAttribute("type", "get_" + userType.getName());
                toolbarCategory.appendChild(getNode);
                var setNode = document.createElement("block");
                setNode.setAttribute("type", "set_" + userType.getName());
                toolbarCategory.appendChild(setNode);
                Mythos.buildCustomTypeBlocks(userType);
            });
        },
        buildCustomTypeBlocks: function (userType) {
            Blockly.Blocks['userType_' + userType.getName()] = {
                init: function () {
                    try {
                        var typeConstructor = this;
                        typeConstructor.setPreviousStatement(false);
                        typeConstructor.setNextStatement(false);
                        typeConstructor.setOutput(true, null);
                        typeConstructor.setColour(200);
                        typeConstructor.setTooltip(userType.getComment());
                        typeConstructor.appendDummyInput()
                                .appendField("Create " + userType.getName());
                        Mythos.each(userType.getAttribute(), function (attribute) {
                            typeConstructor.appendValueInput(attribute.getName())
                                    .setAlign(Blockly.ALIGN_RIGHT)
                                    .setCheck(attribute.getType())
                                    .appendField(attribute.getName());
                        });
                    } catch (error) {
                        Mythos.editor.log(error);
                    }
                }
            };
            Blockly.Blocks['set_' + userType.getName()] = {
                init: function () {
                    try {
                        var typeSetter = this;
                        typeSetter.setColour(200);
                        typeSetter.setPreviousStatement(true);
                        typeSetter.setNextStatement(true);
                        typeSetter.setOutput(false);
                        typeSetter.setTooltip(userType.getComment());
                        typeSetter.appendValueInput()
                                .setAlign(Blockly.ALIGN_LEFT)
                                .appendField("Set")
                                .appendField(new Blockly.FieldVariable(userType.getName()), "VAR")
                                .appendField(".")
                                .appendField(Mythos.getAttributeDropdown(userType), "ATTR")
                                .appendField("to");
                    } catch (error) {
                        Mythos.editor.log(error);
                    }
                }
            };
            Blockly.Blocks['get_' + userType.getName()] = {
                init: function () {
                    try {
                        var typeGetter = this;
                        typeGetter.setColour(200);
                        typeGetter.setPreviousStatement(false);
                        typeGetter.setNextStatement(false);
                        typeGetter.setOutput(true, null);
                        typeGetter.setTooltip(userType.getComment());
                        typeGetter.appendDummyInput()
                                .setAlign(Blockly.ALIGN_LEFT)
                                .appendField("Get")
                                .appendField(new Blockly.FieldVariable(userType.getName()), "VAR")
                                .appendField(".")
                                .appendField(Mythos.getAttributeDropdown(userType), "ATTR");
                    } catch (error) {
                        Mythos.editor.log(error);
                    }
                }
            };
        },
        getVariableDropdown: function (userType) {
            var variables = Mythos.editor.getVariablesByType(userType.getName());
            var options = [];
            Mythos.each(variables, function (variable) {
                options.push([variable.getName(), variable.getName()]);
            });
            return new Blockly.FieldDropdown(options);
        },
        getAttributeDropdown: function (userType) {
            var options = [];
            Mythos.each(userType.getAttribute(), function (attribute) {
                options.push([attribute.getName(), attribute.getName()]);
            });
            return new Blockly.FieldDropdown(options);
        },
        addFunctionsFromScope: function (target, prefix, functions) {
            Mythos.each(functions, function (func) {
                var scriptNode = document.createElement("block");
                scriptNode.setAttribute("type", prefix + "_" + func.getName());
                target.appendChild(scriptNode);
                scriptNode = document.createElement("block");
                scriptNode.setAttribute("type", prefix + "ignore_" + func.getName());
                target.appendChild(scriptNode);
                Blockly.Blocks[prefix + 'ignore_' + func.getName()] = {
                    init: function () {
                        this.setPreviousStatement(true);
                        this.setNextStatement(true);
                        this.setColour(250);
                        this.appendDummyInput()
                                .appendField(prefix + " " + func.getName());
                        var functionBlock = this;
                        Mythos.each(Mythos.editor.getParametersForScript(func), function (argName) {
                            functionBlock.appendValueInput(argName)
                                    .setAlign(Blockly.ALIGN_RIGHT)
                                    .setCheck(null)
                                    .appendField(argName);
                        });
                    }
                };
                Blockly.Blocks[prefix + '_' + func.getName()] = {
                    init: function () {
                        this.setColour(250);
                        this.setPreviousStatement(false);
                        this.setNextStatement(false);
                        this.setOutput(true, null);
                        this.appendDummyInput()
                                .appendField(prefix + " " + func.getName());
                        var functionBlock = this;
                        Mythos.each(Mythos.editor.getParametersForScript(func), function (argName) {
                            functionBlock.appendValueInput(argName)
                                    .setAlign(Blockly.ALIGN_RIGHT)
                                    .setCheck(null)
                                    .appendField(argName);
                        });
                    }
                };
            });
        },
        addFunctionsFromGlobalScope: function () {
            var toolbarCategory = document.getElementById("globalFunctions");
            Mythos.addFunctionsFromScope(toolbarCategory, "Global", Mythos.editor.getGlobalFunctions());
        },
        addFunctionsFromLocalScope: function () {
            var toolbarCategory = document.getElementById("localFunctions");
            Mythos.addFunctionsFromScope(toolbarCategory, "Local", Mythos.editor.getLocalFunctions());
        },
        addCustomVariables: function () {
            Blockly.Variables.allVariables_old = Blockly.Variables.allVariables;
            Blockly.Variables.allVariables = function (workspace) {
                var list = Blockly.Variables.allVariables_old(workspace);
                Mythos.each(Mythos.editor.getVariablesByType("String"), function (variable) {
                    if (list.indexOf(variable.getName()) < 0) {
                        list.push(variable.getName());
                    }
                });
                Mythos.each(Mythos.editor.getVariablesByType("Number"), function (variable) {
                    if (list.indexOf(variable.getName()) < 0) {
                        list.push(variable.getName());
                    }
                });
                Mythos.each(Mythos.editor.getVariablesByType("Boolean"), function (variable) {
                    if (list.indexOf(variable.getName()) < 0) {
                        list.push(variable.getName());
                    }
                });
                return list;
            };
        },
        initBlocks: function () {
            Blockly.Blocks['flow_for'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(180);
                    this.appendDummyInput()
                            .appendField("For");
                    this.appendStatementInput("PRE")
                            .setAlign(Blockly.ALIGN_RIGHT)
                            .appendField("Init");
                    this.appendValueInput("CONDITION")
                            .setCheck("Boolean")
                            .appendField("condition");
                    this.appendStatementInput("AFTERTHOUGHT")
                            .setAlign(Blockly.ALIGN_RIGHT)
                            .appendField("after");
                    this.appendStatementInput("BODY")
                            .setAlign(Blockly.ALIGN_RIGHT)
                            .appendField("loop body");
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['flow_repeat'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(180);
                    this.appendDummyInput()
                            .appendField("Repeat");
                    this.appendStatementInput("BODY")
                            .setAlign(Blockly.ALIGN_RIGHT)
                            .appendField("loop body");
                    this.appendValueInput("CONDITION")
                            .setCheck("Boolean")
                            .appendField("until");
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['flow_break'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(180);
                    this.appendDummyInput()
                            .appendField("Break");
                    this.setTooltip('Break out of current loop');
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                }
            };
            Blockly.Blocks['flow_continue'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(180);
                    this.appendDummyInput()
                            .appendField("Continue");
                    this.setTooltip('Restart at top of current loop');
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                }
            };
            Blockly.Blocks['logic_cointoss'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(210);
                    this.appendDummyInput()
                            .appendField("Coin toss");
                }
            };
            Blockly.Blocks['events_set_map'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(false);
                    this.appendDummyInput()
                            .appendField("Set map to")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField('x')
                            .appendField(new Blockly.FieldTextInput("0"), "X")
                            .appendField('y')
                            .appendField(new Blockly.FieldTextInput("0"), "Y")
                            .appendField('facing')
                            .appendField(new Blockly.FieldTextInput("0"), "FACING")
                            .appendField('(0-15)');
                    this.setOutput(false);
                    this.setTooltip('Switch to a different map (by name) and set position on it');
                }
            };
            Blockly.Blocks['events_teleport'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Teleport to")
                            .appendField('x')
                            .appendField(new Blockly.FieldTextInput("0"), "X")
                            .appendField('y')
                            .appendField(new Blockly.FieldTextInput("0"), "Y")
                            .appendField('facing')
                            .appendField(new Blockly.FieldTextInput("0"), "FACING")
                            .appendField('(0-15)');
                    this.setOutput(false);
                    this.setTooltip('Teleport the player to a given location and direction on this map.');
                }
            };
            Blockly.Blocks['events_move_backward'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Move backward");
                    this.setOutput(false);
                    this.setTooltip('Moves the player one step backward.');
                }
            };
            Blockly.Blocks['events_set_sky'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Set sky color to")
                            .appendField(new Blockly.FieldTextInput("0"), "COLOR")
                            .appendField('(0-17)');
                    this.setOutput(false);
                    this.setTooltip('Set color of the sky');
                }
            };
            Blockly.Blocks['events_set_ground'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Set ground color to")
                            .appendField(new Blockly.FieldTextInput("0"), "COLOR")
                            .appendField('(0-17)');
                    this.setOutput(false);
                    this.setTooltip('Set color of the ground');
                }
            };
            Blockly.Blocks['events_add_encounter_zone'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Add encounter zone for enemy code")
                            .appendField(new Blockly.FieldTextInput(""), "CODE")
                            .appendField('at X=')
                            .appendField(new Blockly.FieldTextInput("0"), "X")
                            .appendField('Y=')
                            .appendField(new Blockly.FieldTextInput("0"), "Y")
                            .appendField('with max dist')
                            .appendField(new Blockly.FieldTextInput("0"), "MAXDIST")
                            .appendField('(0=inf), and chance')
                            .appendField(new Blockly.FieldTextInput("0.0"), "CHANCE")
                            .appendField('%');
                    this.setOutput(false);
                    this.setTooltip('Add an encounter zone');
                }
            };
            Blockly.Blocks['events_add_encounter_zone'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Add encounter zone for enemy code")
                            .appendField(new Blockly.FieldTextInput(""), "CODE")
                            .appendField('at X=')
                            .appendField(new Blockly.FieldTextInput("0"), "X")
                            .appendField('Y=')
                            .appendField(new Blockly.FieldTextInput("0"), "Y")
                            .appendField('with max dist')
                            .appendField(new Blockly.FieldTextInput("0"), "MAXDIST")
                            .appendField('(0=inf), and chance')
                            .appendField(new Blockly.FieldTextInput("0.0"), "CHANCE")
                            .appendField('%');
                    this.setOutput(false);
                    this.setTooltip('Add an encounter zone');
                }
            };
            Blockly.Blocks['events_start_encounter'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Start encounter with enemy code")
                            .appendField(new Blockly.FieldTextInput(""), "CODE");
                    this.setOutput(false);
                    this.setTooltip('Start an encounter');
                }
            };
            Blockly.Blocks['events_clr_encounter_zones'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Clear encounter zones");
                    this.setOutput(false);
                    this.setTooltip('Clear all encounter zones (for no encounters, or to add new zones)');
                }
            };
            Blockly.Blocks['text_window'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .setAlign(Blockly.ALIGN_RIGHT)
                            .appendField("Window")
                            .appendField('left')
                            .appendField(new Blockly.FieldTextInput("0"), "left")
                            .appendField('right')
                            .appendField(new Blockly.FieldTextInput("39"), "right");
                    this.appendDummyInput()
                            .setAlign(Blockly.ALIGN_RIGHT)
                            .appendField('top')
                            .appendField(new Blockly.FieldTextInput("0"), "top")
                            .appendField('bottom')
                            .appendField(new Blockly.FieldTextInput("23"), "bottom");
                    this.setOutput(false);
                    this.setTooltip('Define text window boundaries');
                }
            };
            Blockly.Blocks['text_moveto'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Move to")
                            .appendField('x')
                            .appendField(new Blockly.FieldTextInput("0"), "x")
                            .appendField('y')
                            .appendField(new Blockly.FieldTextInput("0"), "y");
                    this.setOutput(false);
                    this.setTooltip('Move text cursor to specified position');
                }
            };
            Blockly.Blocks['text_println'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendValueInput("VALUE")
                            .appendField("Println");
                    this.setOutput(false);
                    this.setTooltip('Print text and advance to next line');
                }
            };
            Blockly.Blocks['text_print'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendValueInput("VALUE")
                            .appendField("Print");
                    this.setOutput(false);
                    this.setTooltip('Print text and leave cursor at end of last printed character');
                }
            };
            Blockly.Blocks['text_getanykey'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Get any key");
                    this.setOutput(false);
                    this.setTooltip('Get a key from the keyboard (and discard it)');
                }
            };
            Blockly.Blocks['text_mode'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    var textModes = new Blockly.FieldDropdown([['Normal', 0], ['Inverse', 1]]);
                    this.appendDummyInput()
                            .appendField("Text Mode")
                            .appendField(textModes, "MODE");
                    this.setOutput(false);
                    this.setTooltip('Print text and leave cursor at end of last printed character');
                }
            };
            Blockly.Blocks['text_clear_window'] = {
                init: function () {
                    this.setHelpUrl('https://docs.google.com/document/d/1VXbiY4G533-cokjQevZFhwvqMMCL--17ziMAoFoeJ5M');
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Clear Window");
                    this.setOutput(false);
                    this.setTooltip('Clears text window and moves cursor to the top');
                }
            };
            Blockly.Blocks['text_scroll'] = {
                init: function () {
                    this.setHelpUrl('https://docs.google.com/document/d/1VXbiY4G533-cokjQevZFhwvqMMCL--17ziMAoFoeJ5M');
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Scroll");
                    this.setOutput(false);
                    this.setTooltip('Scrolls text window up one line');
                }
            };
            Blockly.Blocks['text'] = {
                init: function () {
                    this.setHelpUrl(Blockly.Msg.TEXT_TEXT_HELPURL);
                    this.setColour(Blockly.Blocks.texts.HUE);
                    this.appendDummyInput()
                            .appendField(this.newQuote_(true))
                            .appendField(new Blockly.FieldTextArea('', this.checkSpelling), 'TEXT')
                            .appendField(this.newQuote_(false));
                    this.setOutput(true, 'String');
                    this.setTooltip(Blockly.Msg.TEXT_TEXT_TOOLTIP);
                },
                newQuote_: function (open) {
                    var file;
                    if (open === this.RTL) {
                        file = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAKCAQAAAAqJXdxAAAAqUlEQVQI1z3KvUpCcRiA8ef9E4JNHhI0aFEacm1o0BsI0Slx8wa8gLauoDnoBhq7DcfWhggONDmJJgqCPA7neJ7p934EOOKOnM8Q7PDElo/4x4lFb2DmuUjcUzS3URnGib9qaPNbuXvBO3sGPHJDRG6fGVdMSeWDP2q99FQdFrz26Gu5Tq7dFMzUvbXy8KXeAj57cOklgA+u1B5AoslLtGIHQMaCVnwDnADZIFIrXsoXrgAAAABJRU5ErkJggg==';
                    } else {
                        file = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAKCAQAAAAqJXdxAAAAn0lEQVQI1z3OMa5BURSF4f/cQhAKjUQhuQmFNwGJEUi0RKN5rU7FHKhpjEH3TEMtkdBSCY1EIv8r7nFX9e29V7EBAOvu7RPjwmWGH/VuF8CyN9/OAdvqIXYLvtRaNjx9mMTDyo+NjAN1HNcl9ZQ5oQMM3dgDUqDo1l8DzvwmtZN7mnD+PkmLa+4mhrxVA9fRowBWmVBhFy5gYEjKMfz9AylsaRRgGzvZAAAAAElFTkSuQmCC';
                    }
                    return new Blockly.FieldImage(file, 12, 12, '"');
                },
                checkSpelling: function(value) {                    
                    if (this.sourceBlock_) {
                        if (value !== this.lastSpellCheck_) {
                            this.sourceBlock_.setCommentText(Mythos.editor.checkSpelling(value));
                        }
                        this.lastSpellCheck_ = value;
                    }
                    return value;
                }
            };
            Blockly.Blocks['text_getstring'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.appendDummyInput()
                            .appendField("Get String");
                    this.setOutput(true, "String");
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['text_getnumber'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.appendDummyInput()
                            .appendField("Get Number");
                    this.setOutput(true, "Number");
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['text_getcharacter'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.appendDummyInput()
                            .appendField("Get Character");
                    this.setOutput(true, "Number");
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['text_getboolean'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(Blockly.Blocks.logic.HUE);
                    this.appendDummyInput()
                            .appendField("Get Yes or No");
                    this.setOutput(true, "Boolean");
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['interaction_give_item'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Give")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("to player");
                    this.setOutput(false);
                    this.setTooltip('Give an item to the player');
                }
            };
            Blockly.Blocks['interaction_take_item'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Take")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("from player");
                    this.setOutput(false);
                    this.setTooltip('Take an item away from the player (if possible)');
                }
            };
            Blockly.Blocks['interaction_has_item'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(Blockly.Blocks.logic.HUE);
                    this.appendDummyInput()
                            .appendField("player has item")
                            .appendField(new Blockly.FieldTextInput(""), "NAME");
                    this.setOutput(true, "Boolean");
                    this.setTooltip('Check if player has a given item');
                }
            };
            Blockly.Blocks['interaction_add_player'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Add player")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("to party");
                    this.setOutput(false);
                    this.setTooltip('Add a player to the party');
                }
            };
            Blockly.Blocks['interaction_remove_player'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Remove player")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("from party");
                    this.setOutput(false);
                    this.setTooltip('Remove a player from the party (if possible)');
                }
            };
            Blockly.Blocks['interaction_has_player'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(Blockly.Blocks.logic.HUE);
                    this.appendDummyInput()
                            .appendField("party has player")
                            .appendField(new Blockly.FieldTextInput(""), "NAME");
                    this.setOutput(true, "Boolean");
                    this.setTooltip('Check if party has a given player');
                }
            };
            Blockly.Blocks['interaction_bench_player'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Send a player to the bench");
                    this.setOutput(false);
                    this.setTooltip('Send a player to the bench (selected by user)');
                }
            };
            Blockly.Blocks['interaction_unbench_player'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Retrieve a benched player");
                    this.setOutput(false);
                    this.setTooltip('Retrieve a benched player (selected by user)');
                }
            };
            Blockly.Blocks['interaction_get_stat'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(Blockly.Blocks.math.HUE);
                    this.appendDummyInput()
                            .appendField("player's")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("stat");
                    this.setOutput(true, "Number");
                    this.setTooltip('Get player stat');
                }
            };
            Blockly.Blocks['interaction_increase_stat'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Increase player's")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("stat by")
                            .appendField(new Blockly.FieldTextInput("0"), "AMOUNT");
                    this.setOutput(false);
                    this.setTooltip('Increase stat of player');
                }
            };
            Blockly.Blocks['interaction_decrease_stat'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Decrease player's")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("stat by")
                            .appendField(new Blockly.FieldTextInput("0"), "AMOUNT");
                    this.setOutput(false);
                    this.setTooltip('Decrease stat of player');
                }
            };
            Blockly.Blocks['interaction_increase_party_stats'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Increase entire party's")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("stats by")
                            .appendField(new Blockly.FieldTextInput("0"), "AMOUNT");
                    this.setOutput(false);
                    this.setTooltip('Increase stat of every party member');
                }
            };
            Blockly.Blocks['interaction_decrease_party_stats'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Decrease entire party's")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("stats by")
                            .appendField(new Blockly.FieldTextInput("0"), "AMOUNT");
                    this.setOutput(false);
                    this.setTooltip('Decrease stat of every party member');
                }
            };
            Blockly.Blocks['interaction_get_flag'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(Blockly.Blocks.logic.HUE);
                    this.appendDummyInput()
                            .appendField("game's")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("flag is set");
                    this.setOutput(true, "Boolean");
                    this.setTooltip('Get game flag');
                }
            };
            Blockly.Blocks['interaction_set_flag'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Set game's")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("flag");
                    this.setOutput(false);
                    this.setTooltip('Set a game flag');
                }
            };
            Blockly.Blocks['interaction_clr_flag'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Clear game's")
                            .appendField(new Blockly.FieldTextInput(""), "NAME")
                            .appendField("flag");
                    this.setOutput(false);
                    this.setTooltip('Clear a game flag');
                }
            };
            Blockly.Blocks['interaction_pause'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Pause for")
                            .appendField(new Blockly.FieldTextInput("0.5"), "NUM")
                            .appendField("second(s)");
                    this.setOutput(false);
                    this.setTooltip('Pause for a specified time');
                }
            };
            Blockly.Blocks['graphics_set_portrait'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Display portrait ")
                            .appendField(new Blockly.FieldTextInput(""), "NAME");
                    this.setOutput(false);
                    this.setTooltip('Display the given portait image (by name)');
                }
            };
            Blockly.Blocks['graphics_set_avatar'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Set avatar to tile ")
                            .appendField(new Blockly.FieldTextInput(""), "NAME");
                    this.setOutput(false);
                    this.setTooltip('Use the given tile as the avatar image (by name)');
                }
            };
            Blockly.Blocks['graphics_swap_tile'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Swap tile from X/Y")
                            .appendField(new Blockly.FieldTextInput(""), "FROM_X")
                            .appendField(new Blockly.FieldTextInput(""), "FROM_Y")
                            .appendField("to X/Y")
                            .appendField(new Blockly.FieldTextInput(""), "TO_X")
                            .appendField(new Blockly.FieldTextInput(""), "TO_Y");
                    this.setOutput(false);
                    this.setTooltip('Swap the map tile between two locations');
                }
            };
            Blockly.Blocks['graphics_clr_portrait'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Clear portrait");
                    this.setOutput(false);
                    this.setTooltip('Stop displaying a portrait, return to map display');
                }
            };
            Blockly.Blocks['graphics_set_fullscreen'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Display full screen image ")
                            .appendField(new Blockly.FieldTextInput(""), "NAME");
                    this.setOutput(false);
                    this.setTooltip('Display the given full screen image (by name)');
                }
            };
            Blockly.Blocks['graphics_clr_fullscreen'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Clear full screen image");
                    this.setOutput(false);
                    this.setTooltip('Stop displaying a full screen image, return to map display');
                }
            };
            Blockly.Blocks['graphics_intimate_mode'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(true);
                    this.appendDummyInput()
                            .appendField("Intimate mode")
                            .appendField(new Blockly.FieldDropdown([["begin", "1"], ["end", "0"]]), "FLAG");
                    this.setOutput(false);
                    this.setTooltip('Begin or end intimate mode');
                }
            };
        }
    };
}
;
//------
goog.provide('Blockly.FieldTextArea');
goog.require('Blockly.Field');
goog.require('Blockly.Msg');
goog.require('goog.asserts');
goog.require('goog.userAgent');

/**
 * Class for an editable text field.
 * @param {string} text The initial content of the field.
 * @param {Function} opt_changeHandler An optional function that is called
 *     to validate any constraints on what the user entered.  Takes the new
 *     text as an argument and returns either the accepted text, a replacement
 *     text, or null to abort the change.FFF
 * @param {Function} opt_sourceBlock Pass source block if validation function requires it
 * @extends {Blockly.Field}
 * @constructor
 */
Blockly.FieldTextArea = function (text, opt_changeHandler) {
    this.changeHandler_ = opt_changeHandler;
    Blockly.FieldTextArea.superClass_.constructor.call(this, text, opt_changeHandler);
};
goog.inherits(Blockly.FieldTextArea, Blockly.FieldTextInput);
/**
 * Clone this FieldTextArea.
 * @return {!Blockly.FieldTextArea} The result of calling the constructor again
 *   with the current values of the arguments used during construction.
 */
Blockly.FieldTextArea.prototype.clone = function () {
    return new Blockly.FieldTextArea(this.getText(), this.changeHandler_);
};

Blockly.FieldTextArea.prototype.init = function(block) {
    Blockly.FieldTextArea.superClass_.init.call(this, block);
    if (this.changeHandler_) {
        this.changeHandler_(this.text_);
    }
};

/**
 * Show the inline free-text editor on top of the text.
 * @param {boolean=} opt_quietInput True if editor should be created without
 *     focus.  Defaults to false.
 * @private
 */
Blockly.FieldTextArea.prototype.showEditor_ = function (opt_quietInput) {
    var quietInput = opt_quietInput || false;
    if (!quietInput && (goog.userAgent.MOBILE || goog.userAgent.ANDROID ||
            goog.userAgent.IPAD)) {
        // Mobile browsers have issues with in-line textareas (focus & keyboards).
        var newValue = window.prompt(Blockly.Msg.CHANGE_VALUE_TITLE, this.text_);
        if (this.sourceBlock_ && this.changeHandler_) {
            var override = this.changeHandler_(newValue);
            if (override !== undefined) {
                newValue = override;
            }
        }
        if (newValue !== null) {
            this.setText(newValue);
        }
        return;
    }

    Blockly.WidgetDiv.show(this, this.sourceBlock_.RTL, this.widgetDispose_());
    var div = Blockly.WidgetDiv.DIV;
    // Create the input.
    var htmlInput = goog.dom.createDom('textarea', 'blocklyHtmlInput');
    htmlInput.setAttribute('spellcheck', this.spellcheck_);
    htmlInput.setAttribute('cols', 80);
    htmlInput.setAttribute('rows', 7);
    var fontSize = (Blockly.FieldTextInput.FONTSIZE *
            this.sourceBlock_.workspace.scale) + 'pt';
    div.style.fontSize = fontSize;
    div.style.width = "30em";
    htmlInput.style.width = "30em";
    htmlInput.style.fontSize = fontSize;
    htmlInput.style.backgroundColor = "#eee";
    /** @type {!HTMLTextArea} */
    Blockly.FieldTextInput.htmlInput_ = htmlInput;
    div.appendChild(htmlInput);

    htmlInput.value = htmlInput.defaultValue = this.text_;
    htmlInput.oldValue_ = null;
    this.validate_();
    this.resizeEditor_();
    if (!quietInput) {
        htmlInput.focus();
        htmlInput.select();
    }

    // Bind to keydown -- trap Enter without IME and Esc to hide.
    htmlInput.onKeyDownWrapper_ =
            Blockly.bindEvent_(htmlInput, 'keydown', this, this.onHtmlInputKeyDown_);
    // Bind to keyup -- trap Enter; resize after every keystroke.
    htmlInput.onKeyUpWrapper_ =
            Blockly.bindEvent_(htmlInput, 'keyup', this, this.onHtmlInputChange_);
    // Bind to keyPress -- repeatedly resize when holding down a key.
    htmlInput.onKeyPressWrapper_ =
            Blockly.bindEvent_(htmlInput, 'keypress', this, this.onHtmlInputChange_);
    var workspaceSvg = this.sourceBlock_.workspace.getCanvas();
    htmlInput.onWorkspaceChangeWrapper_ =
            Blockly.bindEvent_(workspaceSvg, 'blocklyWorkspaceChange', this,
                    this.resizeEditor_);
};

/**
 * Handle key down to the editor.
 * @param {!Event} e Keyboard event.
 * @private
 */
Blockly.FieldTextArea.prototype.onHtmlInputKeyDown_ = function (e) {
    var htmlInput = Blockly.FieldTextInput.htmlInput_;
    var escKey = 27;
    if (e.keyCode === escKey) {
        this.setText(htmlInput.defaultValue);
        Blockly.WidgetDiv.hide();
    }
};
/**
 * Handle a change to the editor.
 * @param {!Event} e Keyboard event.
 * @private
 */
Blockly.FieldTextArea.prototype.onHtmlInputChange_ = function (e) {
    var htmlInput = Blockly.FieldTextArea.htmlInput_;
    if (e.keyCode === 27) {
        // Esc
        this.setText(htmlInput.defaultValue);
        Blockly.WidgetDiv.hide();
    } else {
        // Update source block.
        var text = htmlInput.value;
        if (text !== htmlInput.oldValue_) {
            htmlInput.oldValue_ = text;
            this.setText(text);
            this.validate_();
        } else if (goog.userAgent.WEBKIT) {
            // Cursor key.  Render the source block to show the caret moving.
            // Chrome only (version 26, OS X).
            this.sourceBlock_.render();
        }
        this.resizeEditor_();
    }
};