/* global Blockly */

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
        buildCustomType: function (userType) {
            Blockly.Blocks['userType_' + userType.getName()] = {
                init: function () {
                    var typeConstructor = this;
                    typeConstructor.setColour(200);
                    typeConstructor.appendDummyInput()
                            .appendField("Create " + userType.getName());
                    Mythos.each(userType.getAttribute(), function (attribute) {
                        typeConstructor.appendValueInput(attribute.getName())
                                .setAlign(Blockly.ALIGN_RIGHT)
                                .setCheck(attribute.getType())
                                .appendField(attribute.getName());
                    });
                    typeConstructor.setPreviousStatement(true);
                    typeConstructor.setNextStatement(true);
                    typeConstructor.setOutput(true, userType.getName());
                }
            };
            Blockly.Blocks['set_' + userType.getName()] = {
                init: function () {
                    var typeSetter = this;
                    typeSetter.setColour(200);
                    typeSetter.appendValueInput("Set ")
                            .setAlign(Blockly.ALIGN_LEFT)
                            .setCheck(null)
                            .appendField(Mythos.getVariableDropdown(userType), "VAR")
                            .appendField(".")
                            .appendField(Mythos.getAttributeDropdown(userType), "ATTR");
                    typeSetter.setPreviousStatement(true);
                    typeSetter.setNextStatement(true);
                    typeSetter.setOutput(false);
                }
            };
            Blockly.Blocks['get_' + userType.getName()] = {
                init: function () {
                    var typeGetter = this;
                    typeGetter.setColour(200);
                    typeGetter.appendDummyInput()
                            .setAlign(Blockly.ALIGN_LEFT)
                            .setCheck(null)
                            .appendField(Mythos.getVariableDropdown(userType), "VAR")
                            .appendField(".")
                            .appendField(Mythos.getAttributeDropdown(userType), "ATTR");
                    typeGetter.setPreviousStatement(false);
                    typeGetter.setNextStatement(false);
                    typeGetter.setOutput(true, null);
                }
            };
        },
        getVariableDropdown: function (userType) {
            var variables = Mythos.editor.getVariablesByType(userType.getName());
            var options = [];
            Mythos.each(variables, function (variable) {
                options.push([variable.getName(), variable.getName()]);
            });
            return Blockly.FieldDropdown(options);
        },
        getAttributeDropdown: function (userType) {
            var options = [];
            Mythos.each(userType.getAttribute(), function (attribute) {
                options.push([attribute.getName(), attribute.getName()]);
            });
            return Blockly.FieldDropdown(options);
        },
        addFunctionsFromScope: function(target, prefix, functions) {
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
                }
            };
            Blockly.Blocks['flow_continue'] = {
                init: function () {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(180);
                    this.appendDummyInput()
                            .appendField("Continue");
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
                    this.setColour(54);
                    this.appendDummyInput()
                            .appendField("Get Yes or No");
                    this.setOutput(true, "Boolean");
                    this.setTooltip('');
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
        }
    };
}
;
