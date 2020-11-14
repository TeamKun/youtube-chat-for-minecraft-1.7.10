package net.teamfruit.ytchat.gui.config;

import cpw.mods.fml.client.config.GuiButtonExt;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;

import java.util.function.Consumer;

public class PressableButtonEntry extends GuiConfigEntries.ButtonEntry {
    private Consumer<GuiButton> onPress;

    public PressableButtonEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, String title, Consumer<GuiButton> onPress) {
        super(
                owningScreen,
                owningEntryList,
                new ConfigElement<Object>(new Property(I18n.format(title), "", Property.Type.STRING)),
                new GuiButtonExt(0, 0, 0, owningEntryList.controlWidth, 18, I18n.format(title)));
        this.onPress = onPress;
    }

    @Override
    public void updateValueButtonText() {

    }

    @Override
    public void valueButtonPressed(int slotIndex) {
        onPress.accept(btnDefault);
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public void setToDefault() {

    }

    @Override
    public boolean isChanged() {
        return false;
    }

    @Override
    public void undoChanges() {

    }

    @Override
    public boolean saveConfigElement() {
        return false;
    }

    @Override
    public Object getCurrentValue() {
        return null;
    }

    @Override
    public Object[] getCurrentValues() {
        return new Object[0];
    }
}
