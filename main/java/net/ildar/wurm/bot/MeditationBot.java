package net.ildar.wurm.bot;

import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.CreationWindow;
import com.wurmonline.shared.constants.PlayerAction;
import net.ildar.wurm.WurmHelper;
import net.ildar.wurm.Utils;
import net.ildar.wurm.annotations.BotInfo;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

@BotInfo(description =
        "Meditates on the carpet. Assumes that there are no restrictions on meditation skill.",
        abbreviation = "md")
public class MeditationBot extends Bot {
    private long lastRepair;
    private long repairTimeout;
    private float staminaThreshold;
    private int clicks = 3;
    private volatile boolean repairInitiated;
    private volatile int clicked;

    public MeditationBot() {
        registerInputHandler(MeditationBot.InputKey.s, this::setStaminaThreshold);
        registerInputHandler(MeditationBot.InputKey.c, this::setClicksNumber);
        registerInputHandler(MeditationBot.InputKey.rt, this::setRepairTimeout);
    }

    @Override
    protected void work() throws Exception{
        PickableUnit pickableUnit = ReflectionUtil.getPrivateField(WurmHelper.hud.getSelectBar(),
                ReflectionUtil.getField(WurmHelper.hud.getSelectBar().getClass(), "selectedUnit"));
        if (pickableUnit == null || !pickableUnit.getHoverName().contains("meditation rug")) {
            Utils.consolePrint("Select a meditation rug!");
            deactivate();
            return;
        } else
            Utils.consolePrint(this.getClass().getSimpleName() + " will use " + pickableUnit.getHoverName() );
        long carpetId = pickableUnit.getId();
        setRepairTimeout(60000);
        setStaminaThreshold(0.5f);
        registerEventProcessors();
        CreationWindow creationWindow = WurmHelper.hud.getCreationWindow();
        Object progressBar = ReflectionUtil.getPrivateField(creationWindow,
                ReflectionUtil.getField(creationWindow.getClass(), "progressBar"));
        PlayerAction meditationAction = new PlayerAction("",(short) 384, PlayerAction.ANYTHING);
        while (isActive()) {
            waitOnPause();
            if (Math.abs(lastRepair - System.currentTimeMillis()) > repairTimeout) {
                repairInitiated = false;
                int count = 0;
                while(!repairInitiated && count++ < 30) {
                    WurmHelper.hud.sendAction(PlayerAction.REPAIR, carpetId);
                    sleep(1000);
                }
                if (repairInitiated) {
                    lastRepair = System.currentTimeMillis();
                } else
                    Utils.consolePrint("Couldn't repair a meditation rug!");
            }
            clicked = 0;
            while(clicked < clicks) {
                float stamina = WurmHelper.hud.getWorld().getPlayer().getStamina();
                float damage = WurmHelper.hud.getWorld().getPlayer().getDamage();
                if ((stamina+damage) > staminaThreshold) {
                    WurmHelper.hud.sendAction(meditationAction, carpetId);
                }
                sleep(1000);
            }
            sleep(timeout);
        }
    }

    private void registerEventProcessors() {
        registerEventProcessor(message -> message.contains("You repair")
                || message.contains("You start repairing")
                || message.contains("doesn't need repairing")
                || message.contains("you will start repairing"), () -> repairInitiated = true);
        registerEventProcessor(message -> message.contains("You start meditating.")
                || message.contains("you will start meditating again."), () -> clicked++);
    }

    private void setRepairTimeout(String []input){
        if (input == null || input.length != 1) {
            printInputKeyUsageString(MeditationBot.InputKey.rt);
            return;
        }
        try {
            int timeout = Integer.parseInt(input[0]);
            setRepairTimeout(timeout);
        } catch (Exception e) {
            Utils.consolePrint("Wrong timeout value!");
        }
    }

    private void setRepairTimeout(long repairTimeout) {
        if (repairTimeout < 100) {
            Utils.consolePrint("Too small timeout!");
            repairTimeout = 100;
        }
        this.repairTimeout = repairTimeout;
        Utils.consolePrint("Current carpet repair timeout is " + repairTimeout + " milliseconds");
    }

    private void setStaminaThreshold(String input[]) {
        if (input == null || input.length != 1)
            printInputKeyUsageString(MeditationBot.InputKey.s);
        else {
            try {
                float threshold = Float.parseFloat(input[0]);
                setStaminaThreshold(threshold);
            } catch (Exception e) {
                Utils.consolePrint("Wrong threshold value!");
            }
        }
    }

    private void setStaminaThreshold(float s) {
        staminaThreshold = s;
        Utils.consolePrint("Current threshold for stamina is " + staminaThreshold);
    }

    private void setClicksNumber(String input[]) {
        if (input == null || input.length != 1)
            printInputKeyUsageString(MeditationBot.InputKey.c);
        else {
            try {
                int clicks = Integer.parseInt(input[0]);
                setClicks(clicks);
            } catch (Exception e) {
                Utils.consolePrint("Wrong value!");
            }
        }
    }

    private void setClicks(int clicks) {
        this.clicks = clicks;
        Utils.consolePrint(getClass().getSimpleName() + " will do " + clicks + " actions each time");
    }

    private enum InputKey implements Bot.InputKey {
        s("Set the stamina threshold. Player will not do any actions if his stamina is lower than specified threshold",
                "threshold(float value between 0 and 1)"),
        c("Set the amount of actions the bot will do each time", "c(integer value)"),
        rt("Set the meditation rug repair timeout", "timeout(in milliseconds)");

        private String description;
        private String usage;
        InputKey(String description, String usage) {
            this.description = description;
            this.usage = usage;
        }

        @Override
        public String getName() {
            return name();
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getUsage() {
            return usage;
        }
    }
}
