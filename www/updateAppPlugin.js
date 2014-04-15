var cordova = require('cordova'),
    exec = require('cordova/exec');

var UpdateAppPlugin = function() {

};
UpdateAppPlugin.prototype.checkAndUpdate = function(checkPath)
{
    exec(null, null, 'UpdateAppPlugin', 'checkAndUpdate', [checkPath]);
};
UpdateAppPlugin.prototype.check = function(checkPath)
{
    exec(null, null, 'UpdateAppPlugin', 'check', [checkPath]);
};
var updateApp = new UpdateAppPlugin();

module.exports = updateApp;