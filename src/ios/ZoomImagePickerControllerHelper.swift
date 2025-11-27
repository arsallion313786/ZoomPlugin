//
//  ZoomImagePickerControllerHelper.swift
//  ZoomModule
//
//  Created by Waqas Rasheed on 20/09/2022.
//

import UIKit
import Photos
import PhotosUI
import MobileCoreServices

public typealias ZoomImagePickerCompletionHandler = (_ image:UIImage?,_ fileURL:URL?,_ errorTitle:String? ,_ errorDescription:String? ) ->Void
public typealias ZoomImagePickerInfoCompletionHandler = ([UIImagePickerController.InfoKey : Any], _ errorTitle:String? ,_ errorDescription:String? ) ->Void


class ZoomImagePickerControllerHelper:NSObject,UIImagePickerControllerDelegate,UIDocumentPickerDelegate,UINavigationControllerDelegate
{
    
    static  let shared = ZoomImagePickerControllerHelper()
    let picker = UIImagePickerController()
    weak var delegate:UIViewController!
    
    
    
    var imageCompletionhandler:ZoomImagePickerCompletionHandler?
    var imageInfoHandler: ZoomImagePickerInfoCompletionHandler?
    
    func showActionSheetForImageSelection(WithDelegateController controller:UIViewController,showDocument:Bool,allowFiles:[String] = ["public.jpeg","public.png"], infoHandler: ZoomImagePickerInfoCompletionHandler? = nil, WithCompletionHandler handler: @escaping ZoomImagePickerCompletionHandler)
    {
        
        
        //        self.checkPermission()
        //        return;
        
        self.delegate = controller
        self.picker.delegate = self;
        
        self.imageCompletionhandler = handler;
        self.imageInfoHandler = infoHandler
        
        var alertSytle = UIAlertController.Style.actionSheet;
        
        if (UIDevice.current.userInterfaceIdiom == .pad)
        {
            alertSytle = UIAlertController.Style.alert
        }
        
        let actionSheetController = UIAlertController(title: "Please select", message: "", preferredStyle: alertSytle)
        
        
        let cancelActionButton = UIAlertAction(title: "Photo Library", style: .default) { action -> Void in
            self.checkPermission()
        }
        actionSheetController.addAction(cancelActionButton)
        
        let saveActionButton = UIAlertAction(title: "Camera", style: .default) { action -> Void in
            self.checkCameraPermission()
        }
        actionSheetController.addAction(saveActionButton)
        
        if(showDocument)
        {
            let docActionButton = UIAlertAction(title: "Document", style: .default) { action -> Void in
                self.loadDocument(allowFiles: allowFiles);
            }
            actionSheetController.addAction(docActionButton)
        }
        
        let deleteActionButton = UIAlertAction(title: "Cancel", style: .cancel) { action -> Void in
            print("Delete")
        }
        actionSheetController.addAction(deleteActionButton)
        
        controller.present(actionSheetController, animated: true, completion: nil)
    }
    
    
    
    
    func showGalleryImageSelectionVC(WithDelegateController controller:UIViewController,infoHandler: ZoomImagePickerInfoCompletionHandler? = nil)
    {
        self.delegate = controller
        self.picker.delegate = self;
        self.imageInfoHandler = infoHandler
        self.checkPermission()
    }
    
    func showCameraImageSelectionVC(WithDelegateController controller:UIViewController,mediaTypes:[String] = [kUTTypeImage as String],infoHandler: ZoomImagePickerInfoCompletionHandler? = nil)
    {
        self.delegate = controller
        self.picker.delegate = self;
        self.picker.mediaTypes = mediaTypes
        self.imageInfoHandler = infoHandler
        self.checkCameraPermission()
    }
    
    func showDocAttachmentScreen(WithDelegateController controller:UIViewController,allowFiles:[String] = ["public.jpeg","public.png"],WithCompletionHandler handler: @escaping ZoomImagePickerCompletionHandler)
    {
        self.delegate = controller
        self.picker.delegate = self;
        
        self.imageCompletionhandler = handler;
        self.loadDocument(allowFiles: allowFiles);
    }
    
    
    func showCameraForImage(controller:UIViewController,WithCompletionHandler handler: @escaping ZoomImagePickerCompletionHandler)
    {
        self.delegate = controller
        self.picker.delegate = self;
        
        self.imageCompletionhandler = handler
        self.showImageControllerForCamera()
    }
    
    func showAlbumForImage(controller:UIViewController,WithCompletionHandler handler: @escaping ZoomImagePickerCompletionHandler)
    {
        self.delegate = controller
        self.picker.delegate = self;
        
        self.imageCompletionhandler = handler
        self.showImageControllerForPhotoLibrary()
    }
    
    func showCloudFilesForImage(controller:UIViewController,allowFiles:[String],WithCompletionHandler handler: @escaping ZoomImagePickerCompletionHandler) {
        self.delegate = controller
        let documentPickerController = UIDocumentPickerViewController(documentTypes: allowFiles, in: .import)
        documentPickerController.delegate = self
        self.imageCompletionhandler = handler
        controller.present(documentPickerController, animated: true, completion: nil)
    }
    
    func loadDocument(allowFiles:[String] ) {
        let docTypes = allowFiles
            //["public.data"]
            //["public.text","public.plain-text","public.composite-content","public.jpeg","public.html"];
        let picker = UIDocumentPickerViewController(documentTypes: docTypes, in: .open);
        
        
        
        picker.delegate = self
        picker.modalPresentationStyle = .fullScreen
        delegate.present(picker, animated: true, completion: nil);
        
        //public.jpeg public.html ,public.data public.content
        //com.pkware.zip-archive
    }
    
    func checkPermission() {
        let photoAuthorizationStatus = PHPhotoLibrary.authorizationStatus()
        switch photoAuthorizationStatus {
        case .authorized:
            self.showImageControllerForPhotoLibrary()
            print("Access is granted by user")
            break;
        case .notDetermined:
            PHPhotoLibrary.requestAuthorization({
                (newStatus) in
                print("status is \(newStatus)")
                if newStatus ==  PHAuthorizationStatus.authorized {
                    /* do stuff here */
                    DispatchQueue.main.async {
                        self.showImageControllerForPhotoLibrary()
                    }
                    print("success")
                }
            })
            print("It is not determined until now")
            break;
        case .restricted:
            // same same
            ZoomAlert.showAlert(title: "Permission Error", descp: "Sorry, you do not have access  to photo album", controller: delegate)
            print("User do not have access to photo album.")
            break;
            
            
        case .denied:
            // same same
            ZoomAlert.showAlert(title: "Permission Error", descp: "Please allow the application to access your photo album in settings panel of your device", controller: delegate)
            break;
        default:
            break;
            
        }
    }
    
    func checkCameraPermission() {
        let photoAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: AVMediaType.video)
        switch photoAuthorizationStatus {
        case .authorized:
            self.showImageControllerForCamera()
            print("Access is granted by user")
            break;
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: AVMediaType.video) { (newStatus) in
                print("status is \(newStatus)")
                if   AVCaptureDevice.authorizationStatus(for: AVMediaType.video) == AVAuthorizationStatus.authorized
                {
                    print("success")
                    DispatchQueue.main.async {
                        self.showImageControllerForCamera()
                    }
                    
                }
                /* do stuff here */
                
            }
            
            print("It is not determined until now")
            break;
        case .restricted:
            // same same
            print("User do not have access to photo album.")
            ZoomAlert.showAlert(title: "Permission Error", descp: "Sorry, you do not have access to camera on this device", controller: delegate)
            break;
            
        case .denied:
            // same same
            ZoomAlert.showAlert(title: "Permission Error", descp: "Please allow the application to access your camera in settings panel of your device", controller: delegate)
            print("User has denied the permission.")
            break;
        default:
            break;
            
        }
    }
    
    func showImageControllerForPhotoLibrary()
    {
        DispatchQueue.main.async {
            self.picker.allowsEditing = false
            self.picker.sourceType = .photoLibrary
            self.picker.mediaTypes = [/*kUTTypeMovie as String ,kUTTypeVideo as String, */kUTTypeImage as String];
            self.delegate.present(self.picker, animated: true, completion: nil)
        }
       
    }
    
    func showImageControllerForCamera()
    {
        if (!UIImagePickerController.isSourceTypeAvailable(.camera)) {
            ZoomAlert.showAlert(title: "", descp: "Device has no camera", controller: delegate)
            return;
        }
        picker.allowsEditing = false
        picker.sourceType = .camera
        picker.videoQuality = .typeHigh
        picker.mediaTypes = [/*kUTTypeMovie as String,*/ kUTTypeImage as String]
        delegate.present(picker, animated: true, completion: nil)
    }
    
    
    
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        // Local variable inserted by Swift 4.2 migrator.
        //self.delegate.dismiss(animated: true, completion: nil)
        self.delegate.dismiss(animated: true) {
            let infoConverted = convertFromUIImagePickerControllerInfoKeyDictionary(info)
            
            if let chosenImage = infoConverted[convertFromUIImagePickerControllerInfoKey(UIImagePickerController.InfoKey.originalImage)] as? UIImage{
                self.imageCompletionhandler?(chosenImage,nil,nil,nil)
            }
            
            
            self.imageInfoHandler?(info, nil, nil)
        }
        
        
        
    }
    
    
    
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        self.delegate.dismiss(animated: true, completion: nil)
    }
    
    
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        
        
        
        guard
            (controller.documentPickerMode == .open || controller.documentPickerMode == .import),
            let url = urls.first
            
        else {
            return
        }
        //                defer {
        //                    url.stopAccessingSecurityScopedResource()
        //                }
        
        
        let success = url.startAccessingSecurityScopedResource();
        
        
        if(url.lastPathComponent.isImageFile)
        {
            do {
                let data = try Data(contentsOf: url);
                
                
                
                if let image = UIImage(data: data)
                {
                    self.imageCompletionhandler?(image,url,nil,nil)
                    
                }
                else
                {
                    self.imageCompletionhandler?(nil,nil,nil,nil)
                }
                
            }
            catch {
                print(error.localizedDescription)
                if(success){
                    url.stopAccessingSecurityScopedResource()
                }
                
                
                self.imageCompletionhandler?(nil,nil,nil,nil)
            }
            
            
            
        }
        else
        {
            self.imageCompletionhandler?(nil,url,nil,nil)
        }
        
        //        do {
        //            let data = try Data(contentsOf: url);
        //            print(data)
        //        }
        //        catch {
        //            print(error.localizedDescription)
        //        }
    }
    
}

// Helper function inserted by Swift 4.2 migrator.
fileprivate func convertFromUIImagePickerControllerInfoKeyDictionary(_ input: [UIImagePickerController.InfoKey: Any]) -> [String: Any] {
    return Dictionary(uniqueKeysWithValues: input.map {key, value in (key.rawValue, value)})
}

// Helper function inserted by Swift 4.2 migrator.
fileprivate func convertFromUIImagePickerControllerInfoKey(_ input: UIImagePickerController.InfoKey) -> String {
    return input.rawValue
}


extension String{
    var isImageFile:Bool
    {
        
        let fileExtension = NSString(string: self).pathExtension
        if fileExtension  == "jpeg" || fileExtension == "jpg" || fileExtension == "JPG" || fileExtension == "png" || fileExtension == "PNG"
            || fileExtension == "bmp"
        {
            return true
        }
        return false
    }
}
