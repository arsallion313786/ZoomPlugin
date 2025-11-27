//
//  ZoomChatVC.swift
//  NativeBupaZoomApp
//
//  Created by Muhammad Arslan Khalid on 01/10/2024.
//

import UIKit
import ZoomVideoSDK
import MobileCoreServices
import Combine

@available(iOS 13.0, *)
final class ZoomChatVC: BupaBaseVC {
    
    
    
    @IBOutlet private weak var tblView:UITableView!
    @IBOutlet private weak var bottomConstraint:NSLayoutConstraint!
    @IBOutlet private weak var inputField:UITextField!
    
    //@IBOutlet private weak var containerView:UIView!
    
    private var isScreenProtectionAdded = false;
    
    var cancelable = Set<AnyCancellable>()
    
    
    private var currentUserSeletedBaseCode = "";
    
    var fileSelectedListner :((_ data:[String: Any]?, _ error:String?)->Void)?
    var fileDownloadListener :((_ data:[String: Any]?, _ error:String?)->Void)?
    
    private var arrChatMessages:[ZoomVideoSDKChatMessage] = [ZoomVideoSDKChatMessage]();
    private var chatTableHandler:ChatTableHandler!
    
    required init?(arrChatMessages:[ZoomVideoSDKChatMessage], coder: NSCoder) {
        self.arrChatMessages = arrChatMessages
        super.init(coder: coder)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Do any additional setup after loading the view.
        //self.tblView.screenShotPrevension(); 
        self.methodsOnViewLoaded()
       // self.containerView.roundEdges(corners: [.topLeft, .topRight], radius: 35.0)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated);
        currentTopController = self;
    }
    
    
    
    @IBAction func btnDismissPressed(_ sender:UIButton){
        self.dismiss(animated: true, completion: nil)
    }
    
    override func keyboardWillChangeFrame(to frame: CGRect) {
        if(frame != CGRect.zero){
            self.bottomConstraint.constant = -(frame.height - AppConstants.safeArea.bottom);
        }
        else{
            self.bottomConstraint.constant = 0;
        }
        self.view.layoutIfNeeded(animated: true);
        
    }
    
    func reloadData(messages:[ZoomVideoSDKChatMessage]?){
        self.chatTableHandler.reloadData(messages: messages);
    }
    
}

//MARK: Btn Action Methods
@available(iOS 13.0, *)
private extension ZoomChatVC{
    @IBAction func btnSendChatMessagePressed(_ sender:UIButton){
        if(self.inputField.text?.isEmpty == false){
            ZoomVideoSDK.shareInstance()?.getChatHelper()?.sendChat(toAll: self.inputField.text);
            self.inputField.text = "";
        }
    }
    
    @IBAction func btnAttachmentPressed(_ sender:UIButton){
        self.view.endEditing(true);
        ZoomImagePickerControllerHelper.shared.showActionSheetForImageSelection(WithDelegateController: self, showDocument: true, allowFiles: [/*"public.html","public.text","public.plain-text","public.composite-content",*/"public.jpeg","public.png", "public.pdf", "public.composite-content"]) { image, fileURL, errorTitle, errorDescription in
            
            
            //ZoomPluginUtil.shared.fileUploading = true;
            if let image = image{
                DispatchQueue.global(qos: .userInitiated).async {
                    var fileName = "\(Date().timeIntervalSince1970)".replacingOccurrences(of: ".", with: "")
                    fileName = fileName  + ".jpg";
                    
                    let data = image.jpegData(compressionQuality: 1.0)!
                    //DispatchQueue.main.async {
                    DispatchQueue.main.async {
                        self.fileSelectedListner?(self.getJsonObject(baseCode: data.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0)), fileName: fileName, fileType: "image/jpg"), nil);
//                        print("fileName is : \(fileName)")
//                        print("mimtype is : image/jpg)")
                    }
                }
                
            }
            else if let fileURL = fileURL{
                DispatchQueue.global(qos: .userInitiated).async {
                    let data = try? Data.init(contentsOf: fileURL)
                    guard let data = data else {
                        self.fileSelectedListner?(nil, "we are not able to get binary data from file url");
                        return
                    };
                    let fileModel = ZoomFileInfoModel(fileURL: fileURL)
                    
                    
                    DispatchQueue.main.async {
                        self.fileSelectedListner?(self.getJsonObject(baseCode:  data.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0)), fileName: fileModel.fullName, fileType: fileModel.type), nil);
                        
//                        print("fileName is : \(fileModel.fullName ?? "no name")")
//                        print("mimtype is : \(fileModel.type)")
                    }
                }
                
                
            }
            else if let _ = errorTitle, let errorDescription = errorDescription{
                // ZoomPluginUtil.shared.fileUploading = false;
                self.fileSelectedListner?(nil, errorDescription);
            }
        }
    }
}

//MARK: Utility Methods
@available(iOS 13.0, *)
private extension ZoomChatVC{
    func methodsOnViewLoaded(){
        self.configureTablehandler();
        //        self.addRequiredObservers();
    }
    
    //    func addRequiredObservers(){
    //        ZoomPluginUtil.shared.$fileUploading
    //            .sink { [weak self] isUploading in
    //                guard let self = self else {return}
    //                if(isUploading){
    //                    let hud = HKProgressHUD.show(addedToView: self.view, animated: true)
    //                    hud.label?.text = "File Uploading....";
    //
    //                }
    //                else{
    //                    DispatchQueue.main.async {
    //                        _ = HKProgressHUD.hide(addedToView: self.view, animated: true)
    //                    }
    //
    //                }
    //            }
    //            .store(in: &self.cancelable)
    //
    //        ZoomPluginUtil.shared.$fileDownloading
    //            .sink { [weak self] isUploading in
    //                guard let self = self else {return}
    //                if(isUploading){
    //                    let hud = HKProgressHUD.show(addedToView: self.view, animated: true)
    //                    hud.label?.text = "File Downloading....";
    //                }
    //                else{
    //                    DispatchQueue.main.async {
    //                        _ = HKProgressHUD.hide(addedToView: self.view, animated: true)
    //                    }
    //
    //                }
    //            }
    //            .store(in: &self.cancelable)
    //    }
    
    func configureTablehandler(){
        self.chatTableHandler = ChatTableHandler(tblView: self.tblView, messages: self.arrChatMessages, delegate: self);
    }
    
    func mimeTypeForPath(path: String) -> String {
        let url = NSURL(fileURLWithPath: path)
        let pathExtension = url.pathExtension
        
        if let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, pathExtension! as NSString, nil)?.takeRetainedValue() {
            if let mimetype = UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType)?.takeRetainedValue() {
                return mimetype as String
            }
        }
        return "application/octet-stream"
    }
    
    func mimeTypeForExtension(fileExtension: String) -> String {
        
        let pathExtension = fileExtension;
        
        if let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, pathExtension as NSString, nil)?.takeRetainedValue() {
            if let mimetype = UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType)?.takeRetainedValue() {
                return mimetype as String
            }
        }
        return "application/octet-stream"
    }
    
    func getRandomURL(fileName:String, mimType:String, documentId:String?) -> String{
        return "https://www."  + fileName + "/" + mimType + "/" +  (documentId ?? self.buildRandomString());
    }
    
    func buildRandomString(strLen: Int = 17) -> String
    {
        let chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        let resStr = String((0..<strLen).map{_ in chars.randomElement()!})
        return resStr
    }
    
    func getJsonObject(baseCode:String, fileName:String, fileType:String) -> [String:Any]{
        var jsonObject = [String:Any]();
        jsonObject["base64"] = baseCode
        jsonObject["fileName"] = fileName;
        jsonObject["fileMimetype"] = fileType;
        return jsonObject;
    }
}

@available(iOS 13.0, *)
extension ZoomChatVC: ChatTableHandlerDelegate{
    func didTapOnURL(url: String) {
        //ZoomPluginUtil.shared.fileDownloading = true;
        
        if(url.contains("fileupload.bupa.com.sa")){
            let arr = url.components(separatedBy: "#");
            //let extensionType = arr[arr.count-1];
            //let type = arr[arr.count-2];
            
            //let reqArr = arr.last?.components(separatedBy: "%23") ?? [];
            
            guard arr.count >= 4 else {return}
            
            let fileName  = arr[2];
            let documentId = arr[1];
            let type = arr[3];
            var jsonObject = [String:Any]();
            jsonObject["documentId"] = documentId
            jsonObject["fileName"] = fileName
            jsonObject["fileMimetype"] = type
            
            
            if #available(iOS 16.0, *) {
                throttle(.seconds(1)) {
                    DispatchQueue.main.async {
                        self.fileDownloadListener?(jsonObject,nil);
                    }
                }
            } else {
                // Fallback on earlier versions
                //ZoomPluginUtil.shared.fileDownloading = false;
            }
        }
        else{
            guard let properURL = URL(string:url) else {
                return //be safe
            }
            
            UIApplication.shared.open(properURL, options: [:], completionHandler: nil)
        }
        
        
    }
    
    
}


struct ZoomFileInfoModel {
    
    let name: String
    let fileExtention: String
    let url: URL
    let size: UInt64
    let type: String
    var fullName:String!{
        return "\(self.name).\(self.fileExtention)"
    }
    
    init(fileURL: URL) {
        var fileSize : UInt64 = 0
        do {
            let attr = try FileManager.default.attributesOfItem(atPath: fileURL.path)
            fileSize = attr[FileAttributeKey.size] as! UInt64
        } catch {
            print("CollaborationChatViewController:btnAttachment: Error getting file size \(error)")
        }
        let name = fileURL.deletingPathExtension().lastPathComponent
        var mime = "file"
        let _extention = fileURL.pathExtension
        if let extUTI = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, _extention as CFString, nil)?.takeUnretainedValue(),
           let mimeUTI = UTTypeCopyPreferredTagWithClass(extUTI, kUTTagClassMIMEType)?.takeRetainedValue() {
            mime = mimeUTI as String
        }
        self.name = name.replacingOccurrences(of: " ", with: "_")
        self.url = fileURL
        self.size = fileSize
        self.type = mime
        self.fileExtention = _extention
    }
}

import WebKit
class PreviewViewController: UIViewController, WKNavigationDelegate {
    
    var webView: WKWebView!
    private var baseCode64:String
    
    init(baseCode64:String) {
        self.baseCode64 = baseCode64;
        super.init(nibName: nil, bundle: nil);
    }
    
    
    required  init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    
    override func loadView() {
        view = UIView()
        view.backgroundColor = .lightGray
        webView = WKWebView()
        webView.navigationDelegate = self
        view = webView
        
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            webView.leftAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leftAnchor),
            webView.rightAnchor.constraint(equalTo: view.safeAreaLayoutGuide.rightAnchor),
            webView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
        ])
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated);
        DispatchQueue.global(qos: .default).async {
            let url = URL(string: self.baseCode64)!
            let request = URLRequest(url: url)
            DispatchQueue.main.sync {
                _ = self.webView.load(request)
            }
        }
    }
}
