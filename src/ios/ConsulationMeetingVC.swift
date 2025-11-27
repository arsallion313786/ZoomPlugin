//
//  ConsulationMeetingVC.swift
//  NativeBupaZoomApp
//
//  Created by Muhammad Arslan Khalid on 27/09/2024.
//

import UIKit
import ZoomVideoSDK
import ReplayKit
import AudioToolbox


@available(iOS 13.0, *)
class ConsulationMeetingVC: BupaBaseVC {
    
    @IBOutlet private weak var bottomActionView:UIView!
    
    @IBOutlet private weak var lblWaitingMsg:UILabel!
    
    @IBOutlet private weak var otherUserPlaceHolderIcon:UIImageView!
    @IBOutlet private weak var otherUserBGView:UIView!
    @IBOutlet private weak var meUserPlaceHolderIcon:UIImageView!
    
    @IBOutlet private weak var noCameraPlaceholderIconBtn:UIButton!
    
    @IBOutlet private weak var containerZoomView:UIView!
    
    @IBOutlet private weak var zoomView:ZoomView!
    
    @IBOutlet private weak var callDismissbtn:UIButton!
    @IBOutlet private weak var btnSwitchCamera:UIButton!
    @IBOutlet private weak var btnAudioIcon:UIButton!
    @IBOutlet private weak var btnVideoIcon:UIButton!
    @IBOutlet private weak var btnChatIcon:UIButton!
    
    @IBOutlet private weak var thumbnailView:ZoomView!
    @IBOutlet private weak var thumbnailPlaceHolderView:UIView!
    
    //According to Design these btn are not included
    @IBOutlet private weak var btnSpeakerIcon:UIButton!
    @IBOutlet private weak var btnShareScreen:UIButton!
    
    
    @IBOutlet private weak var lblCurrentUserDesignation:UILabel!
    @IBOutlet private weak var lblCurrentUserName:UILabel!
    
    @IBOutlet private weak var timerView:UIView!
    @IBOutlet private weak var lblTimer:UILabel!
    
    @IBOutlet private weak var snackBarView:UIView!
    @IBOutlet private weak var lblTitleSnackbar:UILabel!
    
    
    var fileSelectedListner :((_ data:[String: Any]?, _ error:String?)->Void)?
    var fileDownloadListener :((_ data:[String: Any]?, _ error:String?)->Void)?
    
    
    
    private var timer:Timer!
    private var chatVc:ZoomChatVC?
    private var arrChatMessages:[ZoomVideoSDKChatMessage] = [ZoomVideoSDKChatMessage]();
    private var isSpeakerOn = true;
    private var isUserInteractAudioOption = false;
    private var isSessionStarted = false;
    
    
    
    //this property we will use to share screen
    var sharedExrensionAppBundleId:String?
    
    
    private var isScreenProtectionAdded = false;
    
    
    
    override func viewDidLoad() {
        
        
        super.viewDidLoad()
        AppScreenSheid.shared.configureScreenShield();
        AppScreenSheid.shared.enableScreenShield();
        self.methodsOnViewLoaded();
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated);
        currentTopController = self;
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews();
    }
    
    
    
    
    
    deinit {
        self.removeObserverWhenRouteChanges();
        print("Consulation meeting vc reference removed");
    }
}


//MARK: Btn Actions
@available(iOS 13.0, *)
private extension ConsulationMeetingVC{
    @IBAction func btnCloseSessionPressed(_ sender:UIButton){
        if(self.isSessionStarted){
            if let user = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf(){
                self.askForLeaveSession(user: user)
            }
        }
        
    }
    
    @IBAction func btnAudioTogglePressed(_ sender:UIButton){
        if(self.isSessionStarted){
            self.isUserInteractAudioOption = true;
            let user = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf();
            if(user?.audioStatus()?.audioType != ZoomVideoSDKAudioType.none){
                if(user?.audioStatus()?.isMuted == false){
                    ZoomVideoSDK.shareInstance()?.getAudioHelper()?.muteAudio(user);
                }
                else{
                    ZoomVideoSDK.shareInstance()?.getAudioHelper()?.unmuteAudio(user);
                }
            }
            else{
                ZoomVideoSDK.shareInstance()?.getAudioHelper()?.startAudio();
            }
        }
        
    }
    
    @IBAction func btnSwitchSpeakerPressed(_ sender:UIButton){
        if(self.isSessionStarted){
            let audioSession = AVAudioSession.sharedInstance()
            do {
                if(isSpeakerOn){
                    try audioSession.overrideOutputAudioPort(AVAudioSession.PortOverride.none)
                    isSpeakerOn = false;
                    if(self.isBluetoothDeviceConnected(audioSession: audioSession)){
                        self.btnSpeakerIcon.setImage(UIImage(named: "head_phone_blue"), for: .normal)
                    }
                    else{
                        self.btnSpeakerIcon.setImage(UIImage(named: "speaker_off"), for: .normal)
                    }
                }
                else{
                    try audioSession.overrideOutputAudioPort(AVAudioSession.PortOverride.speaker)
                    isSpeakerOn = true;
                    
                    self.btnSpeakerIcon.setImage(UIImage(named: "speaker_on"), for: .normal)
                }
                
            } catch let error as NSError {
                print("audioSession error: \(error.localizedDescription)")
            }
        }
        //self.btnSpeakerIcon.setImage(UIImage(systemName:isSpeakerOn ? "speaker" :  "speaker.slash"), for: .normal)
    }
    
    @IBAction func btnToggleVideoPressed(_ sender:UIButton){
        if(self.isSessionStarted){
            if(self.thumbnailView.user == nil){
                self.setData();
            }
            
            
            if(self.btnVideoIcon.isSelected){
                self.btnVideoIcon.isSelected = false;
                ZoomVideoSDK.shareInstance()?.getVideoHelper()?.stopVideo();
                self.self.thumbnailPlaceHolderView.isHidden = false;
                self.thumbnailView.isHidden = true;
                self.btnVideoIcon.setImage(UIImage(named: "video_disable_icon"), for: .normal)
            }
            else{
                
                if(self.thumbnailView == nil){
                    self.setData()
                }
                
                
                self.btnVideoIcon.isSelected = true;
                ZoomVideoSDK.shareInstance()?.getVideoHelper()?.startVideo();
                self.thumbnailPlaceHolderView.isHidden = true;
                self.thumbnailView.isHidden = false;
                self.btnVideoIcon.setImage(UIImage(named: "video_enable_icon"), for: .normal)
            }
        }
    }
    
    @IBAction func btnChatPressed(_ sender:UIButton){
        if(self.isSessionStarted){
            self.chatVc = nil;
            
            self.chatVc =  self.storyboard?.instantiateViewController(identifier: "ZoomChatVC", creator: { coder in
                ZoomChatVC(arrChatMessages: self.arrChatMessages, coder: coder);
            })
            //self.chatVc?.modalPresentationStyle = .overFullScreen;
            
            self.chatVc?.fileSelectedListner = self.fileSelectedListner;
            self.chatVc?.fileDownloadListener = self.fileDownloadListener;
            
            
            
            if let vc =  self.chatVc{
                self.present(vc, animated: true)
            }
        }
        
    }
    
    @IBAction func btnSwitchCameraPressed(_ sender:UIButton){
        if(self.isSessionStarted){
            if(self.btnVideoIcon.isSelected){
                DispatchQueue.main.async {
                    ZoomVideoSDK.shareInstance()?.getVideoHelper()?.switchCamera();
                }
            }else{
                self.showSnackbar(message: "Please switch on your camera");
            }
        }
        
    }
    
    @IBAction func btnShareScreenPressed(_ sender:UIButton){
        if(self.isSessionStarted){
            self.checkAndStartShareScreenProcess();
        }
        
    }
}


//MARK: Utility Methods
@available(iOS 13.0, *)
private extension ConsulationMeetingVC{
    func methodsOnViewLoaded(){
        ZoomVideoSDK.shareInstance()?.delegate = self;
        self.setUI();
        if let user = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf(){
            if #available(iOS 15.0, *) {
                SDKPiPHelper.shared().updatePiPVideoUser(user: user, videoType: .videoData)
            } else {
                // Fallback on earlier versions
            }
        }
    }
    
    func setUI(){
        self.callDismissbtn.roundEdges();
        self.btnAudioIcon.roundEdges();
        self.btnChatIcon.roundEdges();
        self.btnVideoIcon.roundEdges();
        self.btnSwitchCamera.roundEdges();
        self.btnSpeakerIcon.roundEdges();
        self.noCameraPlaceholderIconBtn.roundEdges();
        self.bottomActionView.roundEdges(radius: 34.0)
        
        self.timerView.roundEdges(radius: 17.5);
        self.snackBarView.roundEdges(radius: 20);
        
        self.thumbnailPlaceHolderView.isHidden = false;
        self.thumbnailView.isHidden = true;
        self.btnVideoIcon.setImage(UIImage(named: "video_disable_icon"), for: .normal)
        self.btnAudioIcon.setImage(UIImage(named: "mic_disable_icon"), for: .normal);
        self.thumbnailView.roundEdges(radius: 8.0);
        self.thumbnailPlaceHolderView.roundEdges(radius: 8.0)
        self.addObserverWhenRouteChanges();
        //self.btnShareScreen.isHidden = self.sharedExrensionAppBundleId == nil;
    }
    
    func setData(){
        self.thumbnailView.dataType = ZoomVideoSDKVideoType.videoData;
        self.thumbnailView.user = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf();
        if(self.thumbnailView.user != nil){
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                self.thumbnailView.user!.getVideoCanvas()?.subscribe(with: self.thumbnailView, aspectMode: ZoomVideoSDKVideoAspect.letterBox, andResolution: ZoomVideoSDKVideoResolution._Auto);
            }
            
        }
    }
    
    func setDesignationName(user:ZoomVideoSDKUser){
        if user.isHost(){
            self.lblCurrentUserDesignation.text = "General Practitioner"
        }
        else{
            self.lblCurrentUserDesignation.text = "Patient/Attendee"
        }
        
    }
    
    
    
    
    //    func makeHalfCircleIntoBottomActionView(){
    //        let circlePath = UIBezierPath(arcCenter: CGPoint(x: bottomActionView.bounds.size.width / 2, y: 0), radius: 50, startAngle: 0.0, endAngle: -.pi, clockwise: true)
    //
    //        circlePath.append(UIBezierPath(rect: bottomActionView.bounds));
    //        circlePath.close();
    //
    //        let circleShape = CAShapeLayer()
    //        circleShape.path = circlePath.cgPath
    //        circleShape.fillRule = .evenOdd
    //        bottomActionView.layer.mask = circleShape;
    //    }
    
    func onLeave(){
        self.timer?.invalidate();
        self.timer = nil;
        _ = self.zoomView.user;
        self.unsubscribeView(user: self.zoomView.user, view: self.zoomView);
        self.unsubscribeView(user: self.thumbnailView.user, view: self.thumbnailView);
        self.dismiss(animated: true);
        currentTopController = nil;
        AppScreenSheid.shared.disableScreenShield();
    }
    
    func onAudioStatusChange(){
        let user = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf();
        if(isUserInteractAudioOption){
            if(user?.audioStatus()?.audioType != ZoomVideoSDKAudioType.none){
                if(user?.audioStatus()?.isMuted ?? true){
                    self.btnAudioIcon.setImage(UIImage(named: "mic_disable_icon"), for: .normal);
                }
                else{
                    self.btnAudioIcon.setImage(UIImage(named: "mic_enable_icon"), for: .normal);
                }
            }
            else{
                self.btnAudioIcon.setImage(UIImage(named: "mic_disable_icon"), for: .normal);
            }
        }
        else{
            ZoomVideoSDK.shareInstance()?.getAudioHelper()?.muteAudio(user);
        }
    }
    
    func startTimerForVideoConference(){
        let videoStartDate  = Date();
        
        self.timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: {[weak self] (timer) in
            guard let self = self else {return}
            DispatchQueue.main.async {
                let result = Date().dHMS(fromDate: videoStartDate);
                
                self.lblTimer.text = "\(self.getDesignedTimeValue(val: result.h)):\(self.getDesignedTimeValue(val: result.m)):\(self.getDesignedTimeValue(val: result.s))"
            }
        });
    }
    
    func getDesignedTimeValue(val:Int) -> String{
        let formatter = NumberFormatter();
        formatter.minimumIntegerDigits = 0
        formatter.locale = Locale(identifier: "en_US");
        if val <= 9 {
            return "\(formatter.string(from: NSNumber(value: Float("0")!))!)\(formatter.string(from: NSNumber(value: Float("\(val)")!))!)"
        }
        return formatter.string(from: NSNumber(value: val))!
    }
    
    func showSnackbar(message:String){
        self.lblTitleSnackbar.text =  message;
        UIView.animate(withDuration: 0.3) {
            self.snackBarView.alpha = 1.0
        } completion: { isFinished in
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                UIView.animate(withDuration: 0.3) {
                    self.snackBarView.alpha = 0.0
                }
            }
        }
        
    }
    
    func checkAndStartShareScreenProcess(){
        if(ZoomVideoSDK.shareInstance()?.getShareHelper()?.isShareLocked() ?? false){
            self.showSnackbar(message: "Share is locked by admin");
        }
        else if(ZoomVideoSDK.shareInstance()?.getShareHelper()?.isOtherSharing() ?? false){
            self.showSnackbar(message: "Someone else already sharing");
        }
        else {
            
            
            let broadcastView = RPSystemBroadcastPickerView()
            broadcastView.preferredExtension = self.sharedExrensionAppBundleId;
            broadcastView.tag = 1000000;
            self.view.addSubview(broadcastView)
            self.sendTouchDownEventToBroadcastButton()
            
            
        }
        
        //if (ZoomVideoSDK.shareInstance()?.getShareHelper()?.isScreenSharingOut()  ?? false)  == false
        
    }
    
    func sendTouchDownEventToBroadcastButton(){
        
        let broadcastView:RPSystemBroadcastPickerView?  = self.view.viewWithTag(1000000) as? RPSystemBroadcastPickerView
        guard let broadcastView else { return;}
        
        for subView in broadcastView.subviews{
            if subView.isKind(of: UIButton.self){
                let broadcastBtn = subView as! UIButton
                broadcastBtn.sendActions(for: .allTouchEvents)
                break;
                
            }
        }
        
        
    }
    
    func setUserFullScreenCanvas(user:ZoomVideoSDKUser, type:ZoomVideoSDKVideoType){
        if(type == ZoomVideoSDKVideoType.videoData){
            user.getVideoCanvas()?.subscribe(with: self.zoomView, aspectMode: .full_Filled, andResolution: ._Auto)
        }
        else{
            user.getShareCanvas()?.subscribe(with: self.zoomView, aspectMode: .full_Filled, andResolution: ._Auto);
        }
        
        self.zoomView.user = user
        self.zoomView.dataType = type;
        
        if #available(iOS 15.0, *) {
            SDKPiPHelper.shared().updatePiPVideoUser(user: user, videoType: type)
        } else {
            // Fallback on earlier versions
        };
    }
    
    func unsubscribeView(user:ZoomVideoSDKUser?, view:ZoomView){
        
        user?.getVideoCanvas()?.unSubscribe(with: view);
        user?.getShareCanvas()?.unSubscribe(with: view);
        
        view.user = nil
        view.dataType = nil;
    }
    
    func askForLeaveSession(user:ZoomVideoSDKUser){
        let alert = UIAlertController(title: "Video Session", message: "Please Select an Option", preferredStyle: .actionSheet)
        
        alert.addAction(UIAlertAction(title: "Leave", style: .default , handler:{ (UIAlertAction)in
            ZoomVideoSDK.shareInstance()?.leaveSession(false)
        }))
        
        if(user.isHost()){
            alert.addAction(UIAlertAction(title: "End Session", style: .destructive , handler:{ (UIAlertAction)in
                ZoomVideoSDK.shareInstance()?.leaveSession(true)
            }))
        }
        
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel , handler:{ (UIAlertAction)in
        }))
        
        
        //uncomment for iPad Support
        //if you are also supporting for iPad than you need to provide btn reference
        //also to present action sheet in popover
        //alert.popoverPresentationController?.sourceView = self.view
        
        self.present(alert, animated: true, completion: {
            //print("completion block")
        })
    }
    
    func updateViewIfUserStopVideo(user:ZoomVideoSDKUser, canvas:ZoomView){
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            if(user.getShareCanvas()?.videoStatus()?.on == false && canvas.dataType != ZoomVideoSDKVideoType.shareData){
                //                canvas.backgroundColor = UIColor.black;
                //                self.unsubscribeView(user: user, view: canvas);
                
                
                //self.otherUserPlaceHolderIcon.isHidden = false;
                self.otherUserBGView.isHidden = false;
                self.zoomView.backgroundColor = UIColor(hexString: "0D1846", alpha: 0.6);
                
                if let user = self.thumbnailView.user {
                    if #available(iOS 15.0, *) {
                        SDKPiPHelper.shared().updatePiPVideoUser(user: user, videoType: .videoData)
                    } else {
                        // Fallback on earlier versions
                    }
                }
                
            }
            else{
                if let user = self.zoomView.user {
                    if #available(iOS 15.0, *) {
                        SDKPiPHelper.shared().updatePiPVideoUser(user: user, videoType: .videoData)
                    } else {
                        // Fallback on earlier versions
                    }
                }
                self.otherUserPlaceHolderIcon.isHidden = true;
                self.otherUserBGView.isHidden = true;
            }
        }
        
    }
    
    
    //    func checkIfAlreadyWeHaveUserInSession(){
    //        let arr:[ZoomVideoSDKUser] =  ZoomVideoSDK.shareInstance()?.getSession()?.getRemoteUsers() ?? [];
    //        if(arr.isEmpty == false){
    //
    //        }
    //    }
    
}


//MARK: Audio Session Methods
@available(iOS 13.0, *)
private extension ConsulationMeetingVC{
    
    func addObserverWhenRouteChanges(){
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(handleRouteChange),
                                               name: AVAudioSession.routeChangeNotification,
                                               object: AVAudioSession.sharedInstance())
    }
    
    func removeObserverWhenRouteChanges(){
        NotificationCenter.default.removeObserver(#selector(handleRouteChange), name: AVAudioSession.routeChangeNotification, object: AVAudioSession.sharedInstance());
    }
    
    @objc func handleRouteChange(notification: Notification) {
        guard let userInfo = notification.userInfo,
              let reasonValue = userInfo[AVAudioSessionRouteChangeReasonKey] as? UInt,
              let reason = AVAudioSession.RouteChangeReason(rawValue: reasonValue) else {
            return
        }
        
        switch reason {
        case .newDeviceAvailable:
            let audioSession = AVAudioSession.sharedInstance()
            let val = self.isBluetoothDeviceConnected(audioSession: audioSession);
            if(val){
                try? audioSession.overrideOutputAudioPort(AVAudioSession.PortOverride.none)
                self.btnSpeakerIcon.setImage(UIImage(named: "head_phone_blue"), for: .normal)
                self.isSpeakerOn = false;
            }
            //print("New device available")
            // Handle new device connection
        case .oldDeviceUnavailable:
            print("Old device unavailable")
            let audioSession = AVAudioSession.sharedInstance()
            let val = self.isBluetoothDeviceConnected(audioSession: audioSession);
            if(val == false){
                //print("head phone disconnected");
                self.btnSpeakerIcon.setImage(UIImage(named: "speaker_off"), for: .normal)
            }
            // Handle device disconnection
        case .override:
            print("Route override occurred")
        case .categoryChange:
            let audioSession = AVAudioSession.sharedInstance()
            let val = self.isBluetoothDeviceConnected(audioSession: audioSession);
            if(val){
                try? audioSession.overrideOutputAudioPort(AVAudioSession.PortOverride.none)
                self.btnSpeakerIcon.setImage(UIImage(named: "head_phone_blue"), for: .normal)
                self.isSpeakerOn = false;
               // print("head phone connected");
            }
            //print("New device available")
            // Handle route override
        default:
            
            // Handle other route change reasons
            print("Other route change reason: \(reason)")
        }
    }
    
    func isBluetoothDeviceConnected(audioSession: AVAudioSession) -> Bool {
        let bluetoothPortTypes: Set<AVAudioSession.Port> = [.bluetoothA2DP, .bluetoothLE,
                                                            .bluetoothHFP, .headphones]
        for output in audioSession.currentRoute.outputs {
            if bluetoothPortTypes.contains(output.portType) {
                return true
            }
        }
        return false
    }
}

//MARK: ZoomVide Delegate Methods
@available(iOS 13.0, *)
extension ConsulationMeetingVC:ZoomVideoSDKDelegate{
    func onError(_ ErrorType: ZoomVideoSDKError, detail details: Int) {
        switch(ErrorType){
        case ZoomVideoSDKError.Errors_Session_Join_Failed:
            self.onLeave();
            break;
        case .Errors_Session_Disconnecting:
            break;
        case .Errors_Session_Reconnecting:
            break;
        default:
            break;
        }
    }
    
    
    
    func onSessionJoin() {
        print("Session Joined Successfully");
        isSessionStarted = true;
        self.startTimerForVideoConference();
        
        DispatchQueue.global(qos: .userInitiated).async {
            CallKitManager.shared().startCall(sessionName: ZoomVideoSDK.shareInstance()?.getSession()?.getName()) {
                DispatchQueue.main.async {
                    if #available(iOS 15.0, *) {
                        SDKPiPHelper.shared().presetPiPWithSrcView(sourceView: self.containerZoomView)
                    } else {
                        // Fallback on earlier versions
                    };
                }
                
            }
        }
    }
    
    func onSessionLeave(_ reason: ZoomVideoSDKSessionLeaveReason) {
        if #available(iOS 15.0, *) {
            SDKPiPHelper.shared().cleanUpPictureInPicture()
        } else {
            // Fallback on earlier versions
        };
        CallKitManager.shared().endCall();
        self.onLeave();
        self.isSessionStarted = false;
    }
    
    func onUserJoin(_ helper: ZoomVideoSDKUserHelper?, users userArray: [ZoomVideoSDKUser]?) {
        
        if let meUser = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf(){
            if(meUser.getID() == userArray?.first?.getID()){
                return;
            }
        }
        
        
        if let user = userArray?.first as? ZoomVideoSDKUser{
            self.setDesignationName(user: user);
            self.lblWaitingMsg.isHidden = true;
            self.setUserFullScreenCanvas(user: user, type: .videoData);
            if let name =  user.getName(){
                self.lblCurrentUserName.text = name;
                showSnackbar(message: "\(name) Joined");
            }
        }
    }
    
    func onUserLeave(_ helper: ZoomVideoSDKUserHelper?, users userArray: [ZoomVideoSDKUser]?) {
        
        if let meUser = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf(){
            if(meUser.getID() == userArray?.first?.getID()){
                return;
            }
        }
        
        
        
        if let user = userArray?.first as? ZoomVideoSDKUser{
            
            
            if(user.getID() == self.zoomView.user?.getID()){
                self.unsubscribeView(user: user, view: self.zoomView)
            }
            
            if let name = user.getName(){
                //showSnackbar(message: "\(name) leave session");
                self.lblWaitingMsg.text = "\(name) leave session";
            }
            else{
                //showSnackbar(message: "someone leave session");
                self.lblWaitingMsg.text = "someone leave session";
            }
            self.lblWaitingMsg.isHidden = false;
            
            //self.otherUserPlaceHolderIcon.isHidden = false;
            
            
        }
    }
    
    func onUserVideoStatusChanged(_ helper: ZoomVideoSDKVideoHelper?, user userArray: [ZoomVideoSDKUser]?) {
        //            if let meUser = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf(){
        //                if(meUser.getID() == userArray?.first?.getID()){
        //                    return;
        //                    //self.updateViewIfUserStopVideo(user: meUser, canvas: self.thumbnailView);
        //                }
        //            }
        
        if let user = userArray?.first as? ZoomVideoSDKUser{
            if(user.getID() == self.zoomView.user?.getID()){
                self.updateViewIfUserStopVideo(user: user, canvas: self.zoomView)
            }
        }
        
        
    }
    
    func onUserAudioStatusChanged(_ helper: ZoomVideoSDKAudioHelper?, user userArray: [ZoomVideoSDKUser]?) {
        if let user = userArray?.first as? ZoomVideoSDKUser{
            if(user.getID() == ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf()?.getID()){
                self.onAudioStatusChange()
            }
        }
        
    }
    
    func onChatNewMessageNotify(_ helper: ZoomVideoSDKChatHelper?, message chatMessage: ZoomVideoSDKChatMessage?) {
        if let chatMessage{
            self.arrChatMessages.append(chatMessage);
            self.chatVc?.reloadData(messages: self.arrChatMessages);
            if (self.chatVc?.isBeingDismissed == true){
                self.showSnackbar(message: "\(chatMessage.senderUser?.getName() ?? "unknown"): \(chatMessage.content ?? "N/A")")
            }
        }
    }
    
    func onUserShareStatusChanged(_ helper: ZoomVideoSDKShareHelper?, user: ZoomVideoSDKUser?, status: ZoomVideoSDKReceiveSharingStatus) {
        if let meUser = ZoomVideoSDK.shareInstance()?.getSession()?.getMySelf(){
            if(user?.getID() == meUser.getID()){
                if (status == ZoomVideoSDKReceiveSharingStatus.start || status == ZoomVideoSDKReceiveSharingStatus.resume){
                    //self.btnShareScreen.setImage(UIImage(systemName: "shareplay"), for: .normal)
                }
                else if(status == ZoomVideoSDKReceiveSharingStatus.stop || status == ZoomVideoSDKReceiveSharingStatus.pause){
                    //self.btnShareScreen.setImage(UIImage(systemName: "shareplay.slash"), for: .normal)
                }
                return
            }
        }
        
        if let user {
            if (status == ZoomVideoSDKReceiveSharingStatus.start || status == ZoomVideoSDKReceiveSharingStatus.resume){
                self.setUserFullScreenCanvas(user: user, type: .shareData);
            }
            else if(status == ZoomVideoSDKReceiveSharingStatus.stop){
                self.setUserFullScreenCanvas(user: user, type: .videoData);
            }
        }
    }
    
    
}
