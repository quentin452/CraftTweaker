/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */

package minetweaker.mc1710.network;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import minetweaker.mc1710.MineTweakerConfig;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 *
 * @author Stan
 */
public class MineTweakerCopyClipboardHandler implements IMessageHandler<MineTweakerCopyClipboardPacket, IMessage> {

    @Override
    public IMessage onMessage(MineTweakerCopyClipboardPacket message, MessageContext ctx) {
        if (Desktop.isDesktopSupported() && MineTweakerConfig.handleDesktopPackets) {
            StringSelection stringSelection = new StringSelection(message.getData());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        }

        return null;
    }
}
