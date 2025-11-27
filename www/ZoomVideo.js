var exec = require('cordova/exec');
var PLUGIN_NAME = "ZoomVideo";

function callNativeFunction(name, args, success, error) {
    success = success || function(){};
    error = error || function(){};
    args = args || [];
    exec(success, error, PLUGIN_NAME, name, args);
}

var zoom = {
    openSession: function(jwtToken, sessionName, userName, domain, enableLog, waitingMessage, success, error) {
        callNativeFunction('openSession', [jwtToken, sessionName, userName, domain, enableLog,waitingMessage], success, error);
    },
    addFileUploadListener: function(success, error) {
        callNativeFunction('addFileUploadListener', [], success, error);
    },
    sendDocumentMetaData:function(documentid,filename,fileMimetype,success,error){
        callNativeFunction('sendDocumentMetaData',[documentid,filename,fileMimetype],success,error);
    },
    addDownloadFileListener:function(success,error){
        callNativeFunction('addDownloadFileListener',[],success,error);
    },
    sendFileData:function(fileName,FileMimetype,Binarydata,isBaseCode64,success,error){
        callNativeFunction('sendFileData',[fileName,FileMimetype,Binarydata, isBaseCode64],success,error);
    }


};



module.exports = zoom;