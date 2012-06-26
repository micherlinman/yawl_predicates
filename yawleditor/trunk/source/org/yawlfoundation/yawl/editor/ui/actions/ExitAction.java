/*
 * Created on 09/10/2003
 * YAWLEditor v1.0 
 *
 * @author Lindsay Bradford
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.yawlfoundation.yawl.editor.ui.actions;

import org.yawlfoundation.yawl.editor.ui.actions.specification.YAWLOpenSpecificationAction;
import org.yawlfoundation.yawl.editor.ui.specification.ArchivingThread;
import org.yawlfoundation.yawl.editor.ui.specification.pubsub.FileState;
import org.yawlfoundation.yawl.editor.ui.swing.TooltipTogglingWidget;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ExitAction extends YAWLOpenSpecificationAction implements TooltipTogglingWidget {

    {
        putValue(Action.SHORT_DESCRIPTION, getEnabledTooltipText());
        putValue(Action.NAME, "Exit");
        putValue(Action.LONG_DESCRIPTION, "Exit the application.");
        putValue(Action.SMALL_ICON, getIconByName("Blank"));
        putValue(Action.MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_X));
    }

    public ExitAction(JComponent menu) {}

    public void actionPerformed(ActionEvent event) {
        ArchivingThread.getInstance().exit();
    }

    public void specificationFileStateChange(FileState state) {
        setEnabled(state != FileState.Busy);
    }

    public String getEnabledTooltipText() {
        return " Exit the application ";
    }

    public String getDisabledTooltipText() {
        return " You cannot exit the application until there the current file operation completes ";
    }
}
