package de.florianmichael.viamcp.gui; // Or your updated package

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsyncVersionSlider extends AbstractSliderButton {
    private final List<ProtocolVersion> availableVersions; // Renamed from 'values' for clarity

    public AsyncVersionSlider(int x, int y, int width, int height) {
        // super(x, y, width, height, initialMessage, initialValue (0.0 to 1.0))
        super(x, y, Math.max(width, 110), height, Component.literal(""), 0.0D);

        // Make a mutable copy of the protocols from ViaLoadingBase
        this.availableVersions = new ArrayList<>(ViaLoadingBase.PROTOCOLS);
        if (this.availableVersions.isEmpty()) {
            // Handle the case where no protocols are loaded, maybe log an error or disable the slider
            this.active = false; // Disable the slider if no versions
            this.setMessage(Component.literal("N/A"));
            // Ensure availableVersions is not null for safety, though ArrayList constructor handles it.
            // If it were null, other methods would crash.
            // For robustness, you might want to throw an IllegalStateException if ViaLoadingBase.PROTOCOLS is empty
            // as the slider is non-functional.
            // For now, we'll let it be disabled.
            return;
        }
        Collections.reverse(this.availableVersions); // Reverse to match original logic (newest first on slider left, oldest on right)

        // Set initial slider position based on current target version
        // The 'value' (0.0 to 1.0) in AbstractSliderButton should correspond to an index in 'availableVersions'
        ProtocolVersion currentTarget = ViaLoadingBase.getInstance().getTargetVersion();
        int initialIndexInReversedList = this.availableVersions.indexOf(currentTarget);

        if (initialIndexInReversedList == -1 && !this.availableVersions.isEmpty()) {
            // Target not found in our (reversed) list, default to the first one (newest)
            initialIndexInReversedList = 0;
            // Optionally, update ViaLoadingBase's target if it was out of sync
            // ViaLoadingBase.getInstance().setTargetVersion(this.availableVersions.get(0));
        } else if (initialIndexInReversedList == -1) { // And availableVersions is empty (already handled by return)
            initialIndexInReversedList = 0; // Should not be reached if active=false path taken
        }


        if (this.availableVersions.size() > 1) {
            this.value = (double) initialIndexInReversedList / (double) (this.availableVersions.size() - 1);
        } else {
            this.value = 0.0; // Only one version, or no versions (slider at start)
        }
        this.value = Mth.clamp(this.value, 0.0D, 1.0D);

        updateMessage(); // Set initial display message
    }

    /**
     * Called when the slider value changes (e.g., by dragging).
     * This is where you apply the change.
     */
    @Override
    protected void applyValue() {
        if (!this.active || this.availableVersions.isEmpty()) {
            return;
        }

        // Calculate index based on current slider value (0.0 to 1.0)
        // The original code used Math.ceil. Let's stick to that for consistency.
        int selectedIndex = (int) Math.ceil(this.value * (this.availableVersions.size() - 1));
        selectedIndex = Mth.clamp(selectedIndex, 0, this.availableVersions.size() - 1);

        ProtocolVersion selectedVersion = this.availableVersions.get(selectedIndex);

        // Check if ViaLoadingBase instance exists to prevent NullPointerException
        if (ViaLoadingBase.getInstance() != null) {
            ViaLoadingBase.getInstance().reload(selectedVersion);
        }
    }

    /**
     * Called to update the display message of the slider.
     */
    @Override
    protected void updateMessage() {
        if (this.availableVersions.isEmpty()) {
            setMessage(Component.literal("N/A"));
            return;
        }

        // Calculate index based on current slider value (0.0 to 1.0)
        int selectedIndex = (int) Math.ceil(this.value * (this.availableVersions.size() - 1));
        selectedIndex = Mth.clamp(selectedIndex, 0, this.availableVersions.size() - 1);

        setMessage(Component.literal(this.availableVersions.get(selectedIndex).getName()));
    }

    /**
     * Custom rendering if needed. AbstractSliderButton handles standard rendering.
     * The original code had custom knob drawing in mouseDragged.
     * AbstractSliderButton draws its own knob. If you need the exact 1.12.2 knob appearance,
     * you'd need to override this and manually blit textures.
     * For now, we'll use the default AbstractSliderButton rendering.
     */
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }


    /**
     * Sets the slider to a specific protocol version.
     * @param protocolId The integer ID of the protocol to set.
     */
    public void setVersion(int protocolId) {
        if (this.availableVersions.isEmpty()) {
            return;
        }

        ProtocolVersion versionToSet = null;
        for (ProtocolVersion pv : this.availableVersions) {
            if (pv.getVersion() == protocolId) {
                versionToSet = pv;
                break;
            }
        }
        if (versionToSet == null) {
            for (ProtocolVersion pv : ViaLoadingBase.PROTOCOLS) {
                if (pv.getVersion() == protocolId) {
                    int idxInReversed = this.availableVersions.indexOf(pv);
                    if (idxInReversed != -1) {
                        versionToSet = this.availableVersions.get(idxInReversed);
                    }
                    break;
                }
            }
        }


        if (versionToSet != null) {
            int indexInReversedList = this.availableVersions.indexOf(versionToSet);
            if (indexInReversedList != -1) {
                if (this.availableVersions.size() > 1) {
                    this.value = (double) indexInReversedList / (double) (this.availableVersions.size() - 1);
                } else {
                    this.value = 0.0;
                }
                this.value = Mth.clamp(this.value, 0.0D, 1.0D);
                updateMessage();
            } else {
                System.err.println("AsyncVersionSlider: Could not set version. Protocol ID " + protocolId + " (as " + (versionToSet != null ? versionToSet.getName() : "null") + ") not found in availableVersions list.");
            }
        } else {
            System.err.println("AsyncVersionSlider: Could not set version. Protocol ID " + protocolId + " not found.");
        }
    }
}