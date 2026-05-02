package ru.arixcompany.ui.altmanager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AltManagerScreen extends Screen {
    private final Screen lastScreen;
    private EditBox usernameField;
    private AltList altList;
    private Button addButton;
    private Button backButton;
    private List<OfflineAccount> accounts;

    public AltManagerScreen(Screen lastScreen) {
        super(Component.literal("Менеджер аккаунтов"));
        this.lastScreen = lastScreen;
        this.accounts = new ArrayList<>();
    }

    @Override
    protected void init() {
        // Создаем список аккаунтов
        this.altList = new AltList(this.minecraft, this.width, this.height - 64, 32, 36);
        this.addWidget(this.altList);

        // Поле для ввода никнейма
        this.usernameField = new EditBox(this.font, this.width / 2 - 100, this.height - 52, 200, 20, Component.literal("Никнейм"));
        this.usernameField.setMaxLength(16);
        this.addWidget(this.usernameField);

        // Кнопка добавления аккаунта
        this.addButton = Button.builder(Component.literal("Добавить"), button -> {
            String username = this.usernameField.getValue().trim();
            if (!username.isEmpty() && !accountExists(username)) {
                OfflineAccount account = new OfflineAccount(username);
                this.accounts.add(account);
                this.altList.refreshList();
                this.usernameField.setValue("");
            }
        }).bounds(this.width / 2 - 100, this.height - 28, 98, 20).build();
        this.addRenderableWidget(this.addButton);

        // Кнопка назад
        this.backButton = Button.builder(Component.literal("Назад"), button -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 + 2, this.height - 28, 98, 20).build();
        this.addRenderableWidget(this.backButton);
    }

    private boolean accountExists(String username) {
        return this.accounts.stream().anyMatch(account -> account.getUsername().equalsIgnoreCase(username));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.altList.render(guiGraphics, mouseX, mouseY, partialTick);
        
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.literal("Никнейм:"), this.width / 2 - 100, this.height - 64, 0xA0A0A0);
        
        this.usernameField.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        this.usernameField.tick();
    }

    public void removeAccount(OfflineAccount account) {
        this.accounts.remove(account);
        this.altList.refreshList();
    }

    public void loginWithAccount(OfflineAccount account) {
        // Создаем новый User объект для оффлайн аккаунта
        User newUser = new User(account.getUsername(), account.getUuid().toString(), "", "", User.Type.LEGACY);
        
        // Устанавливаем новый пользователь в Minecraft
        this.minecraft.user = newUser;
        
        // Возвращаемся в главное меню
        this.minecraft.setScreen(this.lastScreen);
    }

    // Внутренний класс для представления оффлайн аккаунта
    public static class OfflineAccount {
        private final String username;
        private final UUID uuid;

        public OfflineAccount(String username) {
            this.username = username;
            this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
        }

        public String getUsername() {
            return username;
        }

        public UUID getUuid() {
            return uuid;
        }
    }

    // Класс списка аккаунтов
    public class AltList extends ObjectSelectionList<AltList.AltEntry> {
        public AltList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            this.refreshList();
        }

        public void refreshList() {
            this.clearEntries();
            for (OfflineAccount account : AltManagerScreen.this.accounts) {
                this.addEntry(new AltEntry(account));
            }
        }

        @Override
        public int getRowWidth() {
            return 220;
        }

        @Override
        protected int getScrollbarPosition() {
            return super.getScrollbarPosition() + 20;
        }

        public class AltEntry extends ObjectSelectionList.Entry<AltEntry> {
            private final OfflineAccount account;
            private final Button loginButton;
            private final Button deleteButton;

            public AltEntry(OfflineAccount account) {
                this.account = account;
                
                this.loginButton = Button.builder(Component.literal("Войти"), button -> {
                    AltManagerScreen.this.loginWithAccount(this.account);
                }).bounds(0, 0, 60, 20).build();

                this.deleteButton = Button.builder(Component.literal("X"), button -> {
                    AltManagerScreen.this.removeAccount(this.account);
                }).bounds(0, 0, 20, 20).build();
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
                // Отображаем имя пользователя
                guiGraphics.drawString(AltManagerScreen.this.font, this.account.getUsername(), x + 5, y + 8, 0xFFFFFF);
                
                // Позиционируем и отображаем кнопки
                this.loginButton.setPosition(x + entryWidth - 85, y + 2);
                this.loginButton.render(guiGraphics, mouseX, mouseY, partialTick);
                
                this.deleteButton.setPosition(x + entryWidth - 22, y + 2);
                this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.loginButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.deleteButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                return false;
            }

            @Override
            public Component getNarration() {
                return Component.literal("Аккаунт: " + this.account.getUsername());
            }
        }
    }
}