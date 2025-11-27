//
//  SDKPiPWindowView.swift
//  NativeBupaZoomApp
//
//  Created by Muhammad Arslan Khalid on 01/10/2024.
//

import UIKit
import ZoomVideoSDK
import AVFoundation

class SDKPiPWindowView: UIView {
    
    private var activeVideo:UIView!
    private var lastDataUser:ZoomVideoSDKUser!
    private var lastDataType:ZoomVideoSDKVideoType
    
    
    override init(frame: CGRect) {
        self.lastDataType = ZoomVideoSDKVideoType.videoData
        super.init(frame: frame)
        self.initSubViews();
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews();
        let screenSize = self.frame.size;
        if (self.activeVideo != nil && self.activeVideo.superview == self) {
            self.activeVideo.frame = CGRect(origin: CGPoint.zero, size: screenSize)
        }
    }
    
    
    private func initSubViews(){
        let windowSize = self.bounds.size;
        self.activeVideo = UIView(frame: CGRect(origin: CGPoint.zero, size: windowSize));
        self.addSubview(self.activeVideo);
        
    }
    
    private func cancelPreviousSubscription(){
        if (self.lastDataUser == nil) {return};
        if (self.lastDataType == ZoomVideoSDKVideoType.shareData) {
            self.lastDataUser.getShareCanvas()?.unSubscribe(with: self.activeVideo!);
        } else if (self.lastDataType == ZoomVideoSDKVideoType.videoData) {
            self.lastDataUser.getVideoCanvas()?.unSubscribe(with: self.activeVideo!);
        }
    }
    
    
    func startShowActive(user:ZoomVideoSDKUser, videoType:ZoomVideoSDKVideoType){
        self.cancelPreviousSubscription();
        if (videoType == ZoomVideoSDKVideoType.shareData) {
            user.getShareCanvas()?.subscribe(withPiPView: self.activeVideo, aspectMode: ZoomVideoSDKVideoAspect.original, andResolution: ZoomVideoSDKVideoResolution._Auto);
            
        } else {
            
            user.getVideoCanvas()!.subscribe(withPiPView: self.activeVideo, aspectMode: ZoomVideoSDKVideoAspect.original, andResolution: ZoomVideoSDKVideoResolution._Auto);
        }
        self.lastDataUser = user;
        self.lastDataType = videoType;
    }
    func stopShowActive(){
        self.cancelPreviousSubscription();
    }
    
    
    func setSpeakerStates(enabled: Bool)
    {
        let session = AVAudioSession.sharedInstance()
        var _: Error?
//        try? session.setCategory(AVAudioSession.Category.playAndRecord)
//        try? session.setMode(AVAudioSession.Mode.voiceChat)
        if enabled {
            try? session.overrideOutputAudioPort(AVAudioSession.PortOverride.speaker)
        } else {
            try? session.overrideOutputAudioPort(AVAudioSession.PortOverride.none)
        }
        try? session.setActive(true)
    }
}
