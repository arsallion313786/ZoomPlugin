//
//  ZoomChatCell.swift
//  NativeBupaZoomApp
//
//  Created by Muhammad Arslan Khalid on 01/10/2024.
//

import UIKit

class ZoomChatCell: UITableViewCell {
    @IBOutlet weak var lblChatContent:ActiveLabel!
    @IBOutlet weak var lblUsername:UILabel!
    @IBOutlet weak var lblTime:UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        self.lblChatContent.numberOfLines = 0
        self.lblChatContent.enabledTypes = [.url];
        self.lblChatContent.textColor = UIColor.black;
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }
    
}
