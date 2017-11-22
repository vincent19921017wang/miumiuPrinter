var exec = require('cordova/exec');

var CpayPrinter = {
     connect:function (fnSuccess,fnError) {
         exec(fnSuccess, fnError, "Cordova-Plugin-Cpay-Printer", "connect", [name]);
     },
     printTextAsImage:function (fnSuccess,fnError) {
         exec(fnSuccess,fnError,"Cordova-Plugin-Cpay-Printer","print",[text]);
     },
     sayHello:function (fnSuccess,fnError) {
         exec(fnSuccess,fnError,"Cordova-Plugin-Cpay-Printer","sayHello",[hehe]);
     },
     getCardInfo:function (fnSuccess,fnError) {
         exec(fnSuccess,fnError,"Cordova-Plugin-Cpay-Printer","getCardInf",[haha]);
     }
};

module.exports = CpayPrinter;
