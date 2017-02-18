Blockly.JavaScript['forward'] = function(block) {
  var power = block.getFieldValue('power');
  var code = "App.forward('" + power + "');\n";
  return code;
};

Blockly.JavaScript['backward'] = function(block) {
  var power = block.getFieldValue('power');
  var code = "App.backward('" + power + "');\n";
  return code;
};

Blockly.JavaScript['stop'] = function(block) {
  var code = "App.backward('0');\n";
  return code;
};

Blockly.JavaScript['turn left'] = function(block) {
  var code = "App.rotateLeft();\n";
  return code;
};

Blockly.JavaScript['turn right'] = function(block) {
  var code = "App.rotateRight();\n";
  return code;
};

Blockly.JavaScript['camera'] = function(block) {
  var position = block.getFieldValue('position');
  var code = "App.camera('" + position + "');\n";
  return code;
};

Blockly.JavaScript['execute'] = function(block) {
  var code = "App.execute();\n";
  return code;
};

Blockly.JavaScript['wait'] = function(block) {
  var ms = Blockly.JavaScript.valueToCode(block, 'millis', Blockly.JavaScript.ORDER_ATOMIC);
  var code = "App.wait(" + ms + ");\n";
  return code;
};

Blockly.JavaScript['wait'] = function(block) {
  var ms = Blockly.JavaScript.valueToCode(block, 'millis', Blockly.JavaScript.ORDER_ATOMIC);
  var code = "App.wait(" + ms + ");\n";
  return code;
};

Blockly.JavaScript['print'] = function(block) {
  var st = Blockly.JavaScript.valueToCode(block, 'string', Blockly.JavaScript.ORDER_ATOMIC);
  var code = "App.print(" + st + ");\n";
  return code;
};

Blockly.JavaScript['random'] = function(block) {
  var value_start = Blockly.JavaScript.valueToCode(block, 'from', Blockly.JavaScript.ORDER_ATOMIC);
  var value_finish = Blockly.JavaScript.valueToCode(block, 'to', Blockly.JavaScript.ORDER_ATOMIC);
  var code = "mathRandomInt(" + value_start + "," + value_finish + ")";
  return [code, Blockly.JavaScript.ORDER_NONE];
  return code;
};