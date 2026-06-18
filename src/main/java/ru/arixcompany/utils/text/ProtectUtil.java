package ru.arixcompany.utils.text;

import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.misc.Protect;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.IMinecraft;

import java.util.regex.Pattern;

public class ProtectUtil implements IMinecraft {

    private static Protect module;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|ftp://)?(?:[\\w-]+\\.)+[a-zA-Z]{2,}(?:/[\\w./%\\-]*(?:\\?[\\w&=.-]*)?)?\\b"
    );

    private static Protect getModule() {
        if (module != null) return module;
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return null;
        module = Arix.getInstance().getModuleRepo().getModule(Protect.class);
        return module;
    }

    public static String filterText(String text) {
        if (text == null || text.isEmpty()) return text;
        Protect m = getModule();
        if (m == null || !m.isState()) return text;

        String result = text;

        if (m.hideSites.isValue()) {
            result = URL_PATTERN.matcher(result).replaceAll("t.me/ArixEveryDay");
        }

        if (m.nickReplacement.getText() == null || m.nickReplacement.getText().isEmpty()) {
            return result;
        }

        if (m.mode.isSelected("Игрок") && mc.player != null) {
            String myName = mc.player.getGameProfile().name();
            if (myName != null && !myName.isEmpty() && result.contains(myName)) {
                result = result.replace(myName, m.nickReplacement.getText());
            }
        }

        if (m.mode.isSelected("Друзья")) {
            for (FriendRepo.Friend friend : FriendRepo.friends) {
                String name = friend.getName();
                if (name != null && !name.isEmpty() && result.contains(name)) {
                    result = result.replace(name, m.nickReplacement.getText());
                }
            }
        }

        return result;
    }
}
