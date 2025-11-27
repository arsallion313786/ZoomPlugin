import ZoomVideoSDK
//import ZoomVideoSDKUIToolkit

var currentTopController:UIViewController?


@available(iOS 13.0, *)
@objc(ZoomVideo) class ZoomVideo: CDVPlugin{
    var emptyMessage: String?
    var isSDKInitilise:Bool = false;
    var fileUploadCallBackId:String?
    var fileDownloadallBackId:String?
    private weak var consulationMeetingVC:ConsulationMeetingVC?
    
    
    @objc(openSession:)
    func openSession(command: CDVInvokedUrlCommand) {
        
        let JWTToken = command.arguments[0] as? String ?? ""
        let sessionName = command.arguments[1] as? String ?? ""
        let userName = command.arguments[2] as? String ?? ""
        let domain = command.arguments[3] as? String ?? "zoom.us"
        let enableLog = command.arguments[4] as? Bool ?? false
        emptyMessage = command.arguments[5] as? String ?? "Waiting for someone to join the call..."
        let groupId = "";
        //command.arguments[5] as? String ?? ""//groupId
        let shareExtensionBundleId = "";
        //command.arguments[6] as? String ?? ""//groupId
        
        
        if(isSDKInitilise == false){
            if initializeZoomSDK(domain: domain, enableLog: enableLog, appGroupId: groupId){
                self.isSDKInitilise = true;
            }
        }
        
        if(isSDKInitilise){
            if joinZoomSession(jwt: JWTToken, sessionName: sessionName, userName: userName){
                openVideoCall(shareExtensionBundleId: shareExtensionBundleId);
            }
        }
    }
    
    
    @objc(addFileUploadListener:)
    func addFileUploadListener(command: CDVInvokedUrlCommand){
        self.fileUploadCallBackId = command.callbackId;
        print("listnern called");
    }
    
    @objc(sendDocumentMetaData:)
    func sendDocumentMetaData(command: CDVInvokedUrlCommand) {
        DispatchQueue.main.async {
            let documentid = command.arguments[0] as? String ?? ""
            let filename = command.arguments[1] as? String ?? ""
            let fileMimetype = command.arguments[2] as? String ?? ""
            
            //let extensionFile = fileMimetype.components(separatedBy: "/").last
            
            ZoomVideoSDK.shareInstance()?.getChatHelper()?.sendChat(toAll: self.getRandomURL(fileName: filename, mimType: fileMimetype, documentId: documentid));
            
            
            //ZoomPluginUtil.shared.fileUploading = false
        }
        
        
        
    }
    
    @objc(handleSuccessErrorMessage:)
    func handleSuccessAndErrorMessage(command: CDVInvokedUrlCommand){
        DispatchQueue.main.async {
            let alertType = command.arguments[0] as? String ?? ""
            let alertMessage = command.arguments[1] as? String ?? ""
            ZoomAlert.showAlert(title: "Consultation", descp: alertMessage, controller: self.consulationMeetingVC?.topMostViewController() ??  self.viewController)
        }
    }
    
    private  func getRandomURL(fileName:String, mimType:String, documentId:String?) -> String{
        let url = "https://fileupload.bupa.com.sa/#" + (documentId ?? self.buildRandomString()) + "#" + fileName + "#" + mimType;
        
        return url;
    }
    
    private  func buildRandomString(strLen: Int = 17) -> String
    {
        let chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        let resStr = String((0..<strLen).map{_ in chars.randomElement()!})
        return resStr
    }
    
    @objc(addDownloadFileListener:)
    func addDownloadFileListener(command: CDVInvokedUrlCommand) {
        self.fileDownloadallBackId = command.callbackId;
        print("listnern called for download");
    }
    
    @objc(sendFileData:)
    func sendFileData(command: CDVInvokedUrlCommand) {
        //let fileName = command.arguments[0] as? String ?? ""
        
        
        //let displayBaseCode64 =  "data:\(MimeType);base64," + BinaryData.base64EncodedString()
        
        
        
        DispatchQueue.main.async {
            _ = command.arguments[0] as? String ?? ""
            let MimeType = command.arguments[1] as? String ?? ""
            let isBaseCode64 = command.arguments[3] as? Bool ?? false
            
            var displayBaseCode64 = "" ;
            if(isBaseCode64){
                let BinaryData = command.arguments[2] as? String ?? "";
                displayBaseCode64 =  "data:\(MimeType);base64," + BinaryData;
                
            }else{
                let BinaryData = command.arguments[2] as? Data ?? Data();
                displayBaseCode64 =  "data:\(MimeType);base64," + BinaryData.base64EncodedString();
            }
            
            let vc =   UIStoryboard(name: "Consultation", bundle: nil).instantiateViewController(identifier: "FilewPreviewVC") { coder in
                FilewPreviewVC(baseCode64: displayBaseCode64, coder: coder);
            }
            if(self.consulationMeetingVC != nil){
                self.consulationMeetingVC?.topMostViewController().present(vc, animated: true)
            }
            else{
                self.viewController.present(vc, animated: true)
            }
            
            //                DispatchQueue.main.async {
            //                    ZoomPluginUtil.shared.fileDownloading = false;
            //                }
            
            
            
            
            
            //            let vc = PreviewViewController(baseCode64: displayBaseCode64)
            //
            //            currentTopController?.present(vc, animated: true)
            
        }
        
        
        
        
        
        
        
    }
    
    
    
    func initializeZoomSDK(domain: String, enableLog: Bool, appGroupId:String) -> Bool{
        let initParams = ZoomVideoSDKInitParams()
        initParams.domain = domain
        if(appGroupId.isEmpty == false){
            initParams.appGroupId = appGroupId
        }
        initParams.enableLog = enableLog
        
        let sdkInitReturnStatus = ZoomVideoSDK.shareInstance()?.initialize(initParams)
        switch sdkInitReturnStatus {
        case .Errors_Success:
            print("SDK initialized successfully")
        default:
            if let error = sdkInitReturnStatus {
                print("SDK failed to initialize: \(error)")
                return false
            }
        }
        return true
    };
    
    
    
    func joinZoomSession(jwt: String, sessionName: String, userName: String) -> Bool{
        let sessionContext = ZoomVideoSDKSessionContext()
        sessionContext.token = jwt
        sessionContext.sessionName = sessionName
        sessionContext.userName = userName
        //here we are setting video and audio for video conference
        let audioOption = ZoomVideoSDKAudioOptions();
        audioOption.connect     = true;
        audioOption.mute        = true;
        
        let videoOption = ZoomVideoSDKVideoOptions();
        videoOption.localVideoOn = false;
        videoOption.multitaskingCameraAccessEnabled = true;
        
        sessionContext.videoOption = videoOption;
        sessionContext.audioOption = audioOption;
        
        if let _ = ZoomVideoSDK.shareInstance()?.joinSession(sessionContext) {
            print("session joined successfully")
            return true
        } else {
            print("session failed to join")
            return false
        }
    }
    
    
    func openVideoCall(shareExtensionBundleId:String){
        let pasteboard = UIPasteboard.general
        pasteboard.string = emptyMessage
        let storyboard = UIStoryboard(name: "Consultation", bundle: nil)
        self.consulationMeetingVC = storyboard.instantiateViewController(withIdentifier: "ConsulationMeetingVC") as? ConsulationMeetingVC
        if(shareExtensionBundleId.isEmpty == false){
            self.consulationMeetingVC?.sharedExrensionAppBundleId = shareExtensionBundleId
        }
        self.consulationMeetingVC?.modalPresentationStyle = .fullScreen
        
        self.consulationMeetingVC?.fileSelectedListner = { [weak self] data, error in
            guard let self = self else {return};
            if let data = data {
                let result = CDVPluginResult.init(status: CDVCommandStatus.ok, messageAs:data);
                result?.setKeepCallbackAs(true);
                self.commandDelegate.send(result, callbackId: self.fileUploadCallBackId);
            }
            else if let error = error{
                let result = CDVPluginResult.init(status: CDVCommandStatus.error, messageAs:error);
                result?.setKeepCallbackAs(true);
                self.commandDelegate.send(result, callbackId: self.fileUploadCallBackId);
            }
            
            
        }
        
        self.consulationMeetingVC?.fileDownloadListener = { [weak self] data, error in
            guard let self = self else {return};
            if let data = data {
                let result = CDVPluginResult.init(status: CDVCommandStatus.ok, messageAs:data);
                result?.setKeepCallbackAs(true);
                self.commandDelegate.send(result, callbackId: self.fileDownloadallBackId);
            }
            else if let error = error{
                let result = CDVPluginResult.init(status: CDVCommandStatus.error, messageAs:error);
                result?.setKeepCallbackAs(true);
                self.commandDelegate.send(result, callbackId: self.fileDownloadallBackId);
            }
            
            
        }
        
        guard let vc = self.self.consulationMeetingVC else {return}
        
        self.viewController.present(vc, animated: true, completion: nil)
    }
    
    /*
     * This action uses the ZoomVideoSDKUIToolkit.xcframework dependency, but this framework is currently in beta as Jan 2024. WHat it does is it replaces our custom screen with a default Zoom video screen provided by Zoom
     */
    /*
     func openVideoCallZoomLayout(jwt: String, sessionName: String, userName: String){
     let vc = UIToolkitVC(sessionContext: SessionContext(jwt: jwt, sessionName: sessionName, username: userName))
     vc.modalPresentationStyle = .fullScreen
     self.viewController.present(vc, animated: true)
     print("session joined")
     }*/
}


extension UIApplication {
    @objc func topMostViewController() -> UIViewController? {
        
        if #available(iOS 13.0, *) {
            if let wind = self.windows.first {
                return wind.rootViewController?.topMostViewController()
            }
            else {
                fatalError("not found window")
            }
        }
        else {
            return self.keyWindow?.rootViewController?.topMostViewController()
        }
        
    }
    
    @objc func windowRootController() -> UIViewController? {
        let window = UIApplication.shared.windows.filter {$0.isKeyWindow}.first
        return window?.rootViewController
    }
}

extension UIViewController {
    @objc  func topMostViewController() -> UIViewController {
        if self.presentedViewController == nil {
            return self
        }
        if let navigation = self.presentedViewController as? UINavigationController {
            return navigation.visibleViewController!.topMostViewController()
        }
        if let tab = self.presentedViewController as? UITabBarController {
            if let selectedTab = tab.selectedViewController {
                return selectedTab.topMostViewController()
            }
            return tab.topMostViewController()
        }
        return self.presentedViewController!.topMostViewController()
    }
    
    var rootNavVC:UINavigationController?{
        if let nav = UIApplication.shared.topMostViewController() as? UINavigationController{
            return nav
        }
        else if let nav = ( UIApplication.shared.topMostViewController())?.navigationController {
            return nav
        }
        return nil
    }
    
}






import UIKit
import SwiftUI


// MARK: UIKit
public class ScreenShield {
    
    public static let shared = ScreenShield()
    private var blurView: UIVisualEffectView?
    private var recordingObservation: NSKeyValueObservation?
    private var blockingScreenMessage: String = "Screen recording not allowed"
    
    public func protect(window: UIWindow) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0, execute: {
            window.setScreenCaptureProtection()
        })
    }
    
    public func protect(view: UIView) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0, execute: {
            view.setScreenCaptureProtection()
        })
    }
    
    public func protectFromScreenRecording(_ blockingScreenMessage: String? = nil) {
        recordingObservation =  UIScreen.main.observe(\UIScreen.isCaptured, options: [.new]) { [weak self] screen, change in
            
            if let errMessage = blockingScreenMessage {
                self?.blockingScreenMessage = errMessage
            }
            
            let isRecording = change.newValue ?? false
            
            if isRecording {
                self?.addBlurView()
            } else {
                self?.removeBlurView()
            }
        }
    }
    
    public func removeScreenRecordingObserver(){
//        recordingObservation?.removeObserver(UIScreen.main, forKeyPath: UIScreen.capturedDidChangeNotification.rawValue);
    }
    
    private func addBlurView() {
        let blurEffect = UIBlurEffect(style: .regular)
        let blurView = UIVisualEffectView(effect: blurEffect)
        blurView.frame = UIScreen.main.bounds
        
        // Add a label to the blur view
        let label = UILabel()
        label.text = self.blockingScreenMessage
        label.font = UIFont.boldSystemFont(ofSize: 20)
        label.textColor = .black
        label.translatesAutoresizingMaskIntoConstraints = false
        blurView.contentView.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: blurView.contentView.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: blurView.contentView.centerYAnchor)
        ])
        
        self.blurView = blurView
        UIApplication.shared.windows.first { $0.isKeyWindow }?.addSubview(blurView)
    }
    
    private func removeBlurView() {
        blurView?.removeFromSuperview()
        blurView = nil
    }
}

extension UIView {
    
    private struct Constants {
        static var secureTextFieldTag: Int { 54321 }
    }
    
    func setScreenCaptureProtection() {
        if viewWithTag(Constants.secureTextFieldTag) is UITextField {
            return
        }
        
        guard superview != nil else {
            for subview in subviews {
                subview.setScreenCaptureProtection()
            }
            return
        }
        
        let secureTextField = UITextField()
        secureTextField.backgroundColor = .clear
        secureTextField.translatesAutoresizingMaskIntoConstraints = false
        secureTextField.tag = Constants.secureTextFieldTag
        secureTextField.isSecureTextEntry = true
        
        insertSubview(secureTextField, at: 0)
        secureTextField.isUserInteractionEnabled = false
        
#if os(iOS)
        layer.superlayer?.addSublayer(secureTextField.layer)
        secureTextField.layer.sublayers?.last?.addSublayer(layer)
        
        secureTextField.topAnchor.constraint(equalTo: self.topAnchor, constant: 0).isActive = true
        secureTextField.bottomAnchor.constraint(equalTo: self.bottomAnchor, constant: 0).isActive = true
        secureTextField.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 0).isActive = true
        secureTextField.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: 0).isActive = true
#else
        secureTextField.frame = bounds
        secureTextField.wantsLayer = true
        secureTextField.layer?.addSublayer(layer!)
        addSubview(secureTextField)
#endif
    }
}



//MARK:  SwiftUI
public struct ProtectScreenshot: ViewModifier {
    @available(iOS 13.0.0, *)
    public func body(content: Content) -> some View {
        ScreenshotProtectView { content }
    }
}

@available(iOS 13.0, *)
public extension View {
    func protectScreenshot() -> some View {
        modifier(ProtectScreenshot())
    }
}

@available(iOS 13.0, *)
struct ScreenshotProtectView<Content: View>: UIViewControllerRepresentable {
    typealias UIViewControllerType = ScreenshotProtectingHostingViewController<Content>
    
    private let content: () -> Content
    
    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }
    
    func makeUIViewController(context: Context) -> UIViewControllerType {
        ScreenshotProtectingHostingViewController(content: content)
    }
    
    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {}
}

@available(iOS 13.0, *)
final class ScreenshotProtectingHostingViewController<Content: View>: UIViewController {
    private let content: () -> Content
    private let wrapperView = ScreenshotProtectingView()
    
    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
        super.init(nibName: nil, bundle: nil)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        view.addSubview(wrapperView)
        wrapperView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            wrapperView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            wrapperView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            wrapperView.topAnchor.constraint(equalTo: view.topAnchor),
            wrapperView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        
            let hostVC = UIHostingController(rootView: content())
        
        hostVC.view.translatesAutoresizingMaskIntoConstraints = false
        
        addChild(hostVC)
        wrapperView.setup(contentView: hostVC.view)
        hostVC.didMove(toParent: self)
    }
}


public final class ScreenshotProtectingView: UIView {
    
    private var contentView: UIView?
    private let textField = UITextField()
    private lazy var secureContainer: UIView? = try? getSecureContainer(from: textField)
    
    public init(contentView: UIView? = nil) {
        self.contentView = contentView
        super.init(frame: .zero)
        setupUI()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        textField.backgroundColor = .clear
        textField.isUserInteractionEnabled = false
        textField.isSecureTextEntry = true
        
        guard let container = secureContainer else { return }
        
        addSubview(container)
        container.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            container.leadingAnchor.constraint(equalTo: leadingAnchor),
            container.trailingAnchor.constraint(equalTo: trailingAnchor),
            container.topAnchor.constraint(equalTo: topAnchor),
            container.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
        
        guard let contentView = contentView else { return }
        setup(contentView: contentView)
    }
    
    public func setup(contentView: UIView) {
        self.contentView?.removeFromSuperview()
        self.contentView = contentView
        
        guard let container = secureContainer else { return }
        
        container.addSubview(contentView)
        container.isUserInteractionEnabled = isUserInteractionEnabled
        contentView.translatesAutoresizingMaskIntoConstraints = false
        
        let bottomConstraint = contentView.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        bottomConstraint.priority = .required - 1
        
        NSLayoutConstraint.activate([
            contentView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            contentView.topAnchor.constraint(equalTo: container.topAnchor),
            bottomConstraint
        ])
    }
    
    func getSecureContainer(from view: UIView) throws -> UIView {
        let containerName: String
        
        if #available(iOS 15, *) {
            containerName = "_UITextLayoutCanvasView"
        } else if #available(iOS 14, *) {
            containerName = "_UITextFieldCanvasView"
        } else if #available(iOS 13, *) {
            containerName = "_UITextFieldContentView"
        }
        else {
            let currentIOSVersion = (UIDevice.current.systemVersion as NSString).floatValue
            throw NSError(domain: "YourDomain", code: -1, userInfo: ["UnsupportedVersion": currentIOSVersion])
        }
        
        let containers = view.subviews.filter { type(of: $0).description() == containerName }
        
        guard let container = containers.first else {
            throw NSError(domain: "YourDomain", code: -1, userInfo: ["ContainerNotFound": containerName])
        }
        
        return container
    }
}


extension UIView {
    func screenShotPrevension() {
        let preventedView = UITextField()
        let view = UIView(frame: CGRect(x: 0, y: 0, width: preventedView.frame.self.width, height: preventedView.frame.self.height))
        preventedView.isSecureTextEntry = true
        self.addSubview(preventedView)
        preventedView.centerYAnchor.constraint(equalTo: self.centerYAnchor).isActive = true
        preventedView.centerXAnchor.constraint(equalTo: self.centerXAnchor).isActive = true
        self.layer.superlayer?.addSublayer(preventedView.layer)
        preventedView.layer.sublayers?.last?.addSublayer(self.layer)
        preventedView.leftView = view
        preventedView.leftViewMode = .always
    }
}
