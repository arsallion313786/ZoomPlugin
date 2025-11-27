//
//  ChatTableHandler.swift
//  NativeBupaZoomApp
//
//  Created by Muhammad Arslan Khalid on 01/10/2024.
//

import UIKit
import ZoomVideoSDK

class ChatTableHandler: NSObject {
    
    private weak var tblView:UITableView!
    private weak var delegate:ChatTableHandlerDelegate!
    private var messages:[ZoomVideoSDKChatMessage]!
    private var imageMapModel = [String:BaseCodeImageModel]();
    init(tblView: UITableView!, messages:[ZoomVideoSDKChatMessage], delegate:ChatTableHandlerDelegate) {
        self.tblView = tblView;
        self.messages = messages;
        self.delegate = delegate;
        super.init();
        self.registerNib();
        self.confireTableView();
    }
    
    
    func reloadData(messages:[ZoomVideoSDKChatMessage]? = nil){
        if let messages{
            self.messages = messages
        }
        self.tblView.reloadData();
    }
    
}


protocol ChatTableHandlerDelegate:AnyObject{
    func didTapOnURL(url:String)
}

//MARK: Utility Methods
extension ChatTableHandler{
    func registerNib(){
        self.tblView.register(UINib(nibName: "ZoomChatCell", bundle: nil), forCellReuseIdentifier: "ZoomChatCell");
    }
    
    func confireTableView(){
        self.tblView.dataSource = self;
        
        self.tblView.estimatedRowHeight = 92;
        self.tblView.rowHeight = UITableView.automaticDimension;
    }
}

//MARK: TableView DataSource Methods
extension ChatTableHandler:UITableViewDataSource{
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        self.messages.count;
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell  = tableView.dequeueReusableCell(withIdentifier: "ZoomChatCell") as? ZoomChatCell else {fatalError("ZoomChatCell not registered")}
        let msg = self.messages[indexPath.row];
        cell.lblUsername.text = msg.senderUser?.getName() ?? "unknown";
        cell.lblTime.text = Date(timeIntervalSince1970: TimeInterval(msg.timeStamp)).getStringFromDate();
        
        cell.lblChatContent.text = msg.content ?? "N/A";
        
//        guard let msgId = msg.messageID else {
//            cell.lblChatContent.text = msg.content ?? "N/A";
//            return cell;
//        }
        
        
//        if imageMapModel.keys.contains(msgId) == false{
//            if  (msg.content ?? "N/A").checkMsgImageBaseCodeEncode(){
//                imageMapModel[msgId] = BaseCodeImageModel(messageId: msgId, url: self.getRandomURL(messageId: msgId), isBaseCodeMsg: true, content: msg.content!)
//                cell.lblChatContent.text = imageMapModel[msgId]!.url;
//                
//            }
//            else{
//                imageMapModel[msgId] = BaseCodeImageModel(messageId: msgId, url: "", isBaseCodeMsg: false, content: msg.content ?? "N/A");
//                cell.lblChatContent.text = msg.content ?? "N/A";
//            }
//        }
//        else{
//            let model =  imageMapModel[msgId];
//            if(model!.isBaseCodeMsg == false){
//                cell.lblChatContent.text = msg.content ?? "N/A";
//              
//            }
//            else{
//                cell.lblChatContent.text = model!.url;
//            }
//        }
        
        cell.lblChatContent.handleURLTap { url in
            let msg = self.messages[indexPath.row];
            print(msg.content ?? "no content")
            self.delegate.didTapOnURL(url: msg.content ?? url.absoluteString);
        }
        
        //Date(timeIntervalSince1970: TimeInterval(msg.timeStamp)).formatted(.dateTime);
        return cell;
        
    }
}


extension Date{
    func getStringFromDate() -> String{
        let dateFormatter = DateFormatter();
        dateFormatter.dateFormat = "dd/MM/yyyy, hh:mm:ss a";
        dateFormatter.locale = Locale(identifier: "en_US");
        return dateFormatter.string(from: self);
    }
    
    
}

extension String{
    
    func checkMsgImageBaseCodeEncode() -> Bool{
        if self.contains(";base64,") {
            print(self.count)
            print(self);
            return true;
        } else {
            return false;
        }
    }
    
}

class BaseCodeImageModel{
    internal init(messageId: String, url: String, isBaseCodeMsg: Bool, content: String) {
        self.messageId = messageId
        self.url = url
        self.isBaseCodeMsg = isBaseCodeMsg
        self.content = content
        
    }
    
    var messageId:String
    var url:String
    var isBaseCodeMsg:Bool
    var content:String
}


