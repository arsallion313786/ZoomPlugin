//
//  FilewPreviewVC.swift
//  ZoomHelloWorldTwo
//
//  Created by Muhammad Arslan Khalid on 26/11/2024.
//

import UIKit

class FilewPreviewVC: UIViewController {
    
    @IBOutlet private  weak var container:UIView!
    @IBOutlet private weak var activityIndicator:UIActivityIndicatorView!
    
    private var webView:WKWebView!
    private var baseCode64:String
    
    
    // MARK: Initialization
        required init?(baseCode64:String,  coder: NSCoder) {
            self.baseCode64 = baseCode64
            super.init(coder: coder)
        }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        //self.methodsOnViewLoaded()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        if(self.webView == nil){
            self.webView = WKWebView()
            self.webView.navigationDelegate = self;
            self.webView.frame  = CGRect(x: 0, y: 0, width: self.container.bounds.width, height: self.container.bounds.height)
            
            self.container.addSubview(self.webView)
            
            self.activityIndicator.startAnimating()
            DispatchQueue.global(qos: .default).async {
                let url = URL(string: self.baseCode64)!
                let request = URLRequest(url: url)
                DispatchQueue.main.sync {
                    _ = self.webView.load(request)
                }
            }
        }
    }
    
    @IBAction func btnDismissPressed(_ sender:UIButton){
        self.dismiss(animated: true);
    }
}



//MARK: Helping Methods
private extension FilewPreviewVC{
    func methodsOnViewLoaded(){
        self.connfigureWebView()
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            self.webView.bounds = self.container.bounds;
            self.loadWebView()
        }
        
    }
    
    func connfigureWebView(){
        self.webView = WKWebView();
        self.webView.navigationDelegate = self;
        self.webView.translatesAutoresizingMaskIntoConstraints = false;
        self.container.addSubview(self.webView)
        view.addSubview(webView)
           NSLayoutConstraint.activate([
               webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
               webView.topAnchor.constraint(equalTo: view.topAnchor),
               webView.rightAnchor.constraint(equalTo: view.rightAnchor),
               webView.bottomAnchor.constraint(equalTo: view.bottomAnchor)])
        
//        NSLayoutConstraint.activate([
//            webView.topAnchor.constraint(equalTo: self.container.topAnchor),
//            webView.leftAnchor.constraint(equalTo: self.container.leftAnchor),
//            webView.rightAnchor.constraint(equalTo: self.container.rightAnchor),
//            webView.bottomAnchor.constraint(equalTo: self.container.bottomAnchor),
//        ])
    }
    
    func loadWebView(){
        self.activityIndicator.startAnimating()
        DispatchQueue.global(qos: .default).async {
            let url = URL(string: self.baseCode64)!
            let request = URLRequest(url: url)
            DispatchQueue.main.sync {
                _ = self.webView.load(request)
            }
        }
    }
    
}


//MARK:- UIWebViewDelegate Methods
extension FilewPreviewVC: WKNavigationDelegate
{
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        
        print("Web view didStartProvisionalNavigation")
        
        
    }
    
    /* This function will be invoked when the web page content begin to return. */
    func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
        print("Web view didCommit")
    }
    
    /* This function will be invoked when the page content returned successfully. */
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        print("Web view didFinish")
        self.activityIndicator.stopAnimating();
        
    }
    
    /* This function will be invoked when the web view object load page failed. */
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        self.activityIndicator.stopAnimating();
        print("Web view didFailProvisionalNavigation")
    }
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
       decisionHandler(.allow)
    }
}
