//
//  SDKPiPHelper.swift
//  NativeBupaZoomApp
//
//  Created by Muhammad Arslan Khalid on 01/10/2024.
//

import UIKit
import AVKit
import ZoomVideoSDK

@available(iOS 15.0, *)
class SDKPiPHelper: NSObject,AVPictureInPictureControllerDelegate {
    
    var pipController:AVPictureInPictureController!
    var pipVideoCallViewCtrl:AVPictureInPictureVideoCallViewController!
    var pipContentSource:AVPictureInPictureController.ContentSource!
    var pipVideoView:SDKPiPWindowView!
    var videoUser:ZoomVideoSDKUser!
    var videoType:ZoomVideoSDKVideoType!
    var sourceView:UIView!
    
    
    
    
    private static var sDKPiPHelper: SDKPiPHelper = {
        let sDKPiPHelper = SDKPiPHelper()
        return sDKPiPHelper;
    }()
    
    class func shared() -> SDKPiPHelper {
        return sDKPiPHelper;
    }
    
    deinit{
        self.clean();
    }
    
    
    override init() {
        super.init();
        NotificationCenter.default.addObserver(self, selector: #selector(appBecameActive), name: UIApplication.didBecomeActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appMovedToBackground), name: UIApplication.willResignActiveNotification, object: nil)
        
    }
    
    @objc func appMovedToBackground() {
        self.startPiPMode();
    }
    
    @objc func appBecameActive() {
        self.cleanUpPictureInPicture();
        if(SDKPiPHelper.isPiPSupported()){
            self.presetPiPWithSrcView(sourceView: self.sourceView);
        }
    }
    
    class func isPiPSupported() -> Bool{
        let isInCall = CallKitManager.shared().isInCall();
        return AVPictureInPictureController.isPictureInPictureSupported() && isInCall;
    }
    
    func presetPiPWithSrcView(sourceView:UIView){
        if (!SDKPiPHelper.isPiPSupported()) {
            return;
        }
        
        self.clean()
        
        pipVideoCallViewCtrl = AVPictureInPictureVideoCallViewController();
        pipVideoCallViewCtrl.view.backgroundColor = UIColor.black;
        
        let defRect = CGRect(origin: CGPointZero, size: CGSizeMake(280, 210));
        pipVideoCallViewCtrl.view.bounds = defRect;
        
        pipVideoView = SDKPiPWindowView(frame: defRect);
        pipVideoView.translatesAutoresizingMaskIntoConstraints = true;
        //ScreenShield.shared.protect(view: self.pipVideoCallViewCtrl.view)
        
        pipVideoCallViewCtrl.view.addSubview(pipVideoView);
        
         
        
        pipVideoCallViewCtrl!.preferredContentSize = CGSizeMake(pipVideoView.frame.size.width, pipVideoView.frame.size.height);
        
        pipContentSource = AVPictureInPictureController.ContentSource(activeVideoCallSourceView: sourceView, contentViewController: pipVideoCallViewCtrl);
        
        
        
        pipController = AVPictureInPictureController(contentSource: pipContentSource);
        
        pipController.canStartPictureInPictureAutomaticallyFromInline = true;
        pipController.delegate = self;
        
        self.sourceView = sourceView;
    }
    
    func isInPiPMode() -> Bool{
        if(pipController != nil){
            return pipController.isPictureInPictureActive || pipController.isPictureInPictureSuspended;
        }
        return false;
    }
    
    func startPiPMode(){
        if (SDKPiPHelper.isPiPSupported() && !self.isInPiPMode() && self.videoUser != nil && self.videoType.rawValue > 0) {
            if ( pipVideoView != nil){
                pipVideoView.startShowActive(user: self.videoUser, videoType: self.videoType);
            }
            
        }
    }
    
    func cleanUpPictureInPicture(){
        if let pipController = self.pipController {
            pipController.stopPictureInPicture();
            if let pipVideoView =  self.pipVideoView{
                pipVideoView.stopShowActive()
            }
            self.clean();
        }
    }
    
    func clean() {
        if (pipVideoView != nil) {
            pipVideoView = nil;
        }
        
        if (pipVideoCallViewCtrl != nil) {
            pipVideoCallViewCtrl = nil;
        }
        
        if (pipContentSource != nil) {
            pipContentSource = nil;
        }
        
        if (pipController != nil) {
            pipController.delegate = nil;
            pipController = nil;
        }
        
    }
    
    func updatePiPVideoUser(user:ZoomVideoSDKUser, videoType:ZoomVideoSDKVideoType){
        self.videoUser = user;
        self.videoType = videoType;
        if (SDKPiPHelper.isPiPSupported() && self.isInPiPMode() && self.videoUser != nil && self.videoType.rawValue > 0) {
            if (pipVideoView != nil){
                self.pipVideoView.startShowActive(user: self.videoUser, videoType: self.videoType)
            }
        }
    }
    
}
