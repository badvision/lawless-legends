if (typeof Mythos === "undefined") {
    // Hook up the rename function to notify the java editor when changes occur
    Blockly.Procedures.rename_old = Blockly.Procedures.rename;
    Blockly.Procedures.rename = function(name) {
        Mythos.editor.setFunctionName(name);
        return Blockly.Procedures.rename_old.call(this,name);
    };
    
    Mythos = {
        setScriptXml: function(xml) {
            Blockly.mainWorkspace.clear();
            var dom = Blockly.Xml.textToDom(xml);
            Blockly.Xml.domToWorkspace(Blockly.mainWorkspace, dom);
            while (Blockly.mainWorkspace.topBlocks_.length > 1) {
                Blockly.mainWorkspace.topBlocks_[1].dispose();
            }
        },
        getScriptXml: function() {
            return Blockly.Xml.workspaceToDom(Blockly.mainWorkspace).innerHTML;
        },
        helpUrl: 'https://docs.google.com/document/d/1VXbiY4G533-cokjQevZFhwvqMMCL--17ziMAoFoeJ5M/edit#heading=h.yv9dmneqjr2b',
        initBlocks: function() {
            Blockly.Blocks['flow_for'] = {
                init: function() {
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
                init: function() {
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
                init: function() {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(180);
                    this.appendDummyInput()
                            .appendField("Break");
                }
            };
            Blockly.Blocks['flow_continue'] = {
                init: function() {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(180);
                    this.appendDummyInput()
                            .appendField("Continue");
                }
            };
            Blockly.Blocks['logic_cointoss'] = {
                init: function() {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(210);
                    this.appendDummyInput()
                            .appendField("Coin toss");
                }
            };
            Blockly.Blocks['events_setmap'] = {
                init: function() {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.setPreviousStatement(true);
                    this.setNextStatement(false);
                    this.appendValueInput("VALUE")
                            .appendField("Set map");
                    this.setOutput(false);
                    this.setTooltip('Switch to a different map (by name)');
                }
            };
            Blockly.Blocks['text_window'] = {
                init: function() {
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
                init: function() {
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
                init: function() {
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
                init: function() {
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
            Blockly.Blocks['text_mode'] = {
                init: function() {
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
                init: function() {
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
                init: function() {
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
                init: function() {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.appendDummyInput()
                            .appendField("Get String");
                    this.setOutput(true, "String");
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['text_getnumber'] = {
                init: function() {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.appendDummyInput()
                            .appendField("Get Number");
                    this.setOutput(true, "Number");
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['text_getcharacter'] = {
                init: function() {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.appendDummyInput()
                            .appendField("Get Character");
                    this.setOutput(true, "Number");
                    this.setTooltip('');
                }
            };
            Blockly.Blocks['text_getboolean'] = {
                init: function() {
                    this.setHelpUrl(Mythos.helpUrl);
                    this.setColour(54);
                    this.appendDummyInput()
                            .appendField("Get Yes or No");
                    this.setOutput(true, "Boolean");
                    this.setTooltip('');
                }
            };
        }
    };
}
;