//
//  ZoomAlert.swift
//  AssetMgmtModule
//
//  Created by Waqas Rasheed on 04/02/2022.
//

import Foundation
import UIKit

struct ZoomAlert
{
    
    private static func displayAlertActionSheet(withTitle title: String?, andMessage message: String!,_ delegate:UIViewController!, cancelActionHandler: ((UIAlertAction) -> Void)? = nil) {
        
        let alert = UIAlertController(title: title, message: message, preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "OK", style: UIAlertAction.Style.default, handler: cancelActionHandler));
        DispatchQueue.main.async {
            delegate.present(alert, animated: true, completion: nil);
        }
    }
    
    private static func displayAlertActionSheet(withTitle title: String?, andMessage message: String!,successTitle:String, successActionStyle:UIAlertAction.Style = .default,cancelTitle:String,cancelActionStyle:UIAlertAction.Style = .default,_ delegate:UIViewController!,successActionHandler: ((UIAlertAction) -> Void)? = nil ,cancelActionHandler: ((UIAlertAction) -> Void)? = nil) {
        
        let alert = UIAlertController(title: title, message: message, preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: successTitle, style: successActionStyle, handler: successActionHandler));
        alert.addAction(UIAlertAction(title: cancelTitle, style: cancelActionStyle, handler: cancelActionHandler));
        DispatchQueue.main.async {
            delegate.present(alert, animated: true, completion: nil);
        }
    }
    
    static func showAlert(title:String?, descp:String?,controller:UIViewController)
    {
        displayAlertActionSheet(withTitle: title, andMessage: descp, controller)
    }
    
    static func showAlert(title:String?, descp:String?,controller:UIViewController, cancelHandler: @escaping ((UIAlertAction) -> Void))
    {
        displayAlertActionSheet(withTitle: title, andMessage: descp, controller, cancelActionHandler: cancelHandler)
    }
    
    static func showAlert(title:String?, descp:String?,controller:UIViewController,successTitle:String, successActionStyle:UIAlertAction.Style = .default,cancelTitle:String,cancelActionStyle:UIAlertAction.Style = .default ,successHandler: @escaping ((UIAlertAction) -> Void) ,cancelHandler: @escaping ((UIAlertAction) -> Void))
    {
        displayAlertActionSheet(withTitle: title, andMessage: descp, successTitle: successTitle,successActionStyle: successActionStyle, cancelTitle: cancelTitle,cancelActionStyle: cancelActionStyle, controller,successActionHandler: successHandler,cancelActionHandler: cancelHandler)
        
    }
}
