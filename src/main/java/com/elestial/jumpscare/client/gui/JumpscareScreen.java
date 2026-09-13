package com.elestial.jumpscare.client.gui;

import com.elestial.jumpscare.asset.JumpscareAssetManager;
import com.elestial.jumpscare.client.ClientConfigSync;
import com.elestial.jumpscare.client.JumpscareAudioManager;
import com.elestial.jumpscare.client.PossessionControllerHandler;
import com.elestial.jumpscare.config.JumpscareServerConfig;
import com.elestial.jumpscare.network.JumpscareNetworking;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JumpscareScreen extends Screen {
    private int currentTab = 0; // 0 = Visual, 1 = Audio, 2 = Custom, 3 = Kontrol

    private static final Map<String, String> assignments = new LinkedHashMap<>();

    // Selected choices (persisted across menu open/close)
    private static String selectedPlayer = "";
    public static String selectedJumpscareId = "spooky";
    public static String activeGhostAttackId = "spooky";
    private static String selectedControlAction = "spin";

    // Pagination states
    private int playerPage = 0;
    private int idPage = 0;
    private int targetListPage = 0;
    private int audioSoundPage = 0;
    private final List<String> cachedOnlinePlayers = new ArrayList<>();
    private final List<String> cachedValidIds = new ArrayList<>();
    private final List<String> cachedAudioSounds = new ArrayList<>();

    // Tab 0 Widgets (Visual)
    private EditBox targetPlayerBox;
    private EditBox jumpscareIdBox;

    // Tab 1 Widgets (Audio Only)
    private static boolean hideDefaultAudio = false;
    private String selectedAudioSound = "fakestep";
    private boolean audioBehindPlayer = true;
    private EditBox audioTargetBox;
    private EditBox audioSoundBox;
    private EditBox audioUrlBox;

    // Tab 2 Widgets (Tambah Custom)
    private EditBox addIdBox;
    private EditBox addImgUrlBox;
    private EditBox addSoundUrlBox;

    // Tab 3 Widgets (Mind Control)
    private EditBox controlActionBox;

    private String statusMessage = "";
    private int statusColor = 0x55FF55;

    // --- Design constants ---
    private static final int PANEL_BG = 0xF0101018;
    private static final int ACCENT_COLOR = 0xFF9B2335;
    private static final int TEXT_WHITE = 0xFFEEEEEE;
    private static final int TEXT_GRAY = 0xFF999999;
    private static final int TEXT_MUTED = 0xFF555560;

    public enum ButtonStyle {
        DEFAULT,
        PRIMARY,
        DANGER,
        TAB,
        CHIP
    }

    public JumpscareScreen() {
        super(Component.literal("Jumpscare Control Menu"));
    }

    /**
     * Helper to get player's skin ResourceLocation safely.
     */
    private ResourceLocation getPlayerSkin(String playerName) {
        if (this.minecraft != null && this.minecraft.getConnection() != null && playerName != null && !playerName.isEmpty()) {
            for (PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                if (playerName.equalsIgnoreCase(info.getProfile().getName())) {
                    return info.getSkinLocation();
                }
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    /**
     * Renders a player's skin face avatar with hat layer and dark drop shadow.
     */
    private void renderPlayerHead(GuiGraphics g, String playerName, int x, int y, int size) {
        if (playerName == null || playerName.trim().isEmpty()) {
            g.fill(x, y, x + size, y + size, 0x55202028);
            return;
        }
        if ("@a".equalsIgnoreCase(playerName.trim())) {
            g.fill(x, y, x + size, y + size, 0xDD3A0A10);
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xEE551018);
            g.drawCenteredString(this.font, "☠", x + size / 2, y + (size - 8) / 2, 0xFFFF3333);
            return;
        }

        ResourceLocation skin = getPlayerSkin(playerName.trim());
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF101018);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        PlayerFaceRenderer.draw(g, skin, x, y, size);
    }

    private static class GothicButton extends Button {
        private final ButtonStyle style;
        private final boolean selected;
        private final JumpscareScreen screen;

        public GothicButton(JumpscareScreen screen, int x, int y, int width, int height, Component message, OnPress onPress, ButtonStyle style, boolean selected) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.screen = screen;
            this.style = style;
            this.selected = selected;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            screen.renderCustomButton(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.getMessage(), this.isHoveredOrFocused(), this.active, style, selected);
        }
    }

    private static class PlayerCardButton extends Button {
        private final String playerName;
        private final boolean isAssigned;
        private final boolean isSelected;
        private final JumpscareScreen screen;

        public PlayerCardButton(JumpscareScreen screen, int x, int y, int width, int height, String playerName, boolean isAssigned, boolean isSelected, OnPress onPress) {
            super(x, y, width, height, Component.literal(playerName), onPress, DEFAULT_NARRATION);
            this.screen = screen;
            this.playerName = playerName;
            this.isAssigned = isAssigned;
            this.isSelected = isSelected;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int bx = this.getX();
            int by = this.getY();
            int bw = this.getWidth();
            int bh = this.getHeight();
            boolean hovered = this.isHoveredOrFocused();

            int bgColor = isSelected ? 0xDD3E0C16 : (isAssigned ? 0xDD2A1016 : (hovered ? 0xDD222232 : 0xBB12121C));
            int borderColor = isSelected ? 0xFFFF2244 : (isAssigned ? 0xDDFF4455 : (hovered ? 0xFFE53935 : 0x44555566));

            g.fill(bx, by, bx + bw, by + bh, bgColor);
            g.fill(bx, by, bx + bw, by + 1, borderColor);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, borderColor);
            g.fill(bx, by, bx + 1, by + bh, borderColor);
            g.fill(bx + bw - 1, by, bx + bw, by + bh, borderColor);

            int headSize = bh - 4;
            int headY = by + 2;
            screen.renderPlayerHead(g, playerName, bx + 2, headY, headSize);

            int textX = bx + headSize + 4;
            int textY = by + (bh - 8) / 2;
            int maxChars = Math.max(3, (bw - headSize - 10) / 6);
            String displayName = truncate(playerName, maxChars);
            int textColor = isSelected ? 0xFFFFFFFF : (isAssigned ? 0xFFFFCCCC : (hovered ? 0xFFEEEEEE : 0xFFAAAAAA));
            g.drawString(screen.font, displayName, textX, textY, textColor, false);

            if (isAssigned) {
                g.drawString(screen.font, "§c✓", bx + bw - 7, by + 1, 0xFFFF2244, false);
            }
        }
    }

    /**
     * Factory for custom styled gothic/dark obsidian buttons.
     */
    private Button createStyledButton(int x, int y, int width, int height, Component message, Button.OnPress onPress, ButtonStyle style, boolean selected) {
        return new GothicButton(this, x, y, width, height, message, onPress, style, selected);
    }

    /**
     * Factory for player card button with 3D skin head avatar.
     */
    private Button createPlayerCardButton(int x, int y, int width, int height, String playerName, boolean isAssigned, boolean isSelected, Button.OnPress onPress) {
        return new PlayerCardButton(this, x, y, width, height, playerName, isAssigned, isSelected, onPress);
    }

    private void renderCustomButton(GuiGraphics g, int x, int y, int w, int h, Component text, boolean hovered, boolean active, ButtonStyle style, boolean selected) {
        if (!active) {
            g.fill(x, y, x + w, y + h, 0x77121218);
            g.fill(x, y, x + w, y + 1, 0x33333344);
            g.fill(x, y + h - 1, x + w, y + h, 0x33333344);
            g.fill(x, y, x + 1, y + h, 0x33333344);
            g.fill(x + w - 1, y, x + w, y + h, 0x33333344);
            g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, 0xFF555566);
            return;
        }

        switch (style) {
            case PRIMARY -> {
                int topColor = hovered ? 0xFFA8152B : 0xEE880B1E;
                int botColor = hovered ? 0xFF720010 : 0xEE55000A;
                g.fillGradient(x, y, x + w, y + h, topColor, botColor);
                int border = hovered ? 0xFFFF6677 : 0xFFFF2244;
                g.fill(x, y, x + w, y + 1, border);
                g.fill(x, y + h - 1, x + w, y + h, border);
                g.fill(x, y, x + 1, y + h, border);
                g.fill(x + w - 1, y, x + w, y + h, border);
                g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
            }
            case DANGER -> {
                int bg = hovered ? 0xEE44141E : 0xCC260C12;
                int border = hovered ? 0xFFFF3344 : 0x889B2335;
                g.fill(x, y, x + w, y + h, bg);
                g.fill(x, y, x + w, y + 1, border);
                g.fill(x, y + h - 1, x + w, y + h, border);
                g.fill(x, y, x + 1, y + h, border);
                g.fill(x + w - 1, y, x + w, y + h, border);
                g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, hovered ? 0xFFFF8888 : 0xFFFF5555);
            }
            case TAB -> {
                int bg = selected ? 0xDD2A0A10 : (hovered ? 0x9920202E : 0x77101018);
                int border = selected ? 0xCCFF2244 : (hovered ? 0x558B2335 : 0x22333344);
                g.fill(x, y, x + w, y + h, bg);
                g.fill(x, y, x + w, y + 1, border);
                g.fill(x, y, x + 1, y + h, border);
                g.fill(x + w - 1, y, x + w, y + h, border);
                if (selected) {
                    g.fill(x, y + h - 2, x + w, y + h, 0xFFFF2244);
                } else {
                    g.fill(x, y + h - 1, x + w, y + h, border);
                }
                int textColor = selected ? 0xFFFF3344 : (hovered ? 0xFFFFFFFF : 0xFF888899);
                g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, textColor);
            }
            case CHIP -> {
                int bg = selected ? 0xDD3E0C16 : (hovered ? 0xDD222232 : 0x9913131C);
                int border = selected ? 0xFFFF3355 : (hovered ? 0xFFE53935 : 0x44444455);
                g.fill(x, y, x + w, y + h, bg);
                g.fill(x, y, x + w, y + 1, border);
                g.fill(x, y + h - 1, x + w, y + h, border);
                g.fill(x, y, x + 1, y + h, border);
                g.fill(x + w - 1, y, x + w, y + h, border);
                int textColor = selected ? 0xFFFFEEEE : (hovered ? 0xFFFFFFFF : 0xFFAAAAAA);
                g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, textColor);
            }
            default -> {
                int bg = hovered ? 0xDD262638 : 0xCC161622;
                int border = hovered ? 0xFFE53935 : 0x608B2335;
                g.fill(x, y, x + w, y + h, bg);
                g.fill(x, y, x + w, y + 1, border);
                g.fill(x, y + h - 1, x + w, y + h, border);
                g.fill(x, y, x + 1, y + h, border);
                g.fill(x + w - 1, y, x + w, y + h, border);
                int textColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
                g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, textColor);
            }
        }
    }

    private static boolean isValidJumpscareId(String id) {
        if (id == null || id.isBlank()) return false;
        if (id.length() < 2) return false;
        try {
            Integer.parseInt(id);
            return false;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, Math.max(1, maxLen - 1)) + "…";
    }

    private static boolean isDefaultPreset(String id) {
        if (id == null) return false;
        String s = id.toLowerCase(Locale.ROOT);
        return s.equals("fakestep") || s.equals("spooky");
    }

    private static String extractNameFromUrl(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            int qIdx = url.indexOf('?');
            String cleanUrl = (qIdx != -1) ? url.substring(0, qIdx) : url;
            int slashIdx = cleanUrl.lastIndexOf('/');
            String filename = (slashIdx != -1) ? cleanUrl.substring(slashIdx + 1) : cleanUrl;
            int dotIdx = filename.lastIndexOf('.');
            String name = (dotIdx != -1) ? filename.substring(0, dotIdx) : filename;
            name = name.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase(Locale.ROOT);
            if (name.length() > 24) name = name.substring(0, 24);
            return name;
        } catch (Exception ignored) {
            return "";
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int cardWidth = Math.min(380, this.width - 16);
        int cardHeight = Math.min(270, this.height - 16);
        int midX = this.width / 2;
        int midY = this.height / 2;
        int cardLeft = midX - (cardWidth / 2);
        int cardTop = midY - (cardHeight / 2);

        // Fetch Online Players
        cachedOnlinePlayers.clear();
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            for (PlayerInfo playerInfo : this.minecraft.getConnection().getOnlinePlayers()) {
                String name = playerInfo.getProfile().getName();
                if (name != null && !name.isEmpty()) {
                    cachedOnlinePlayers.add(name);
                }
            }
        }
        if (selectedPlayer.isEmpty() && !cachedOnlinePlayers.isEmpty()) {
            selectedPlayer = cachedOnlinePlayers.get(0);
        }

        // Validate Visual IDs
        Set<String> validSet = new LinkedHashSet<>();
        for (Map.Entry<String, ClientConfigSync.EntryData> entry : ClientConfigSync.getAllEntries().entrySet()) {
            String id = entry.getKey();
            if (isValidJumpscareId(id)) {
                if (id.equalsIgnoreCase("spooky") || !entry.getValue().imageUrl.isEmpty()) {
                    validSet.add(id);
                }
            }
        }
        if (validSet.isEmpty() && JumpscareServerConfig.getAllEntries() != null) {
            for (Map.Entry<String, JumpscareServerConfig.JumpscareEntry> entry : JumpscareServerConfig.getAllEntries().entrySet()) {
                String id = entry.getKey();
                if (isValidJumpscareId(id)) {
                    if (id.equalsIgnoreCase("spooky") || (entry.getValue().imageUrl != null && !entry.getValue().imageUrl.isEmpty())) {
                        validSet.add(id);
                    }
                }
            }
        }
        if (validSet.isEmpty()) {
            validSet.add("spooky");
        }

        cachedValidIds.clear();
        cachedValidIds.addAll(validSet);

        // Purge assignments with deleted IDs
        assignments.entrySet().removeIf(entry -> !validSet.contains(entry.getValue().toLowerCase(Locale.ROOT)));
        if (!validSet.contains(selectedJumpscareId.toLowerCase(Locale.ROOT))) {
            selectedJumpscareId = cachedValidIds.get(0);
        }

        // Build available audio sound list
        Set<String> soundSet = new LinkedHashSet<>();
        Set<String> customSounds = new LinkedHashSet<>();

        for (Map.Entry<String, ClientConfigSync.EntryData> entry : ClientConfigSync.getAllEntries().entrySet()) {
            if (isValidJumpscareId(entry.getKey())) {
                customSounds.add(entry.getKey());
            }
        }
        if (JumpscareServerConfig.getAllEntries() != null) {
            for (Map.Entry<String, JumpscareServerConfig.JumpscareEntry> entry : JumpscareServerConfig.getAllEntries().entrySet()) {
                if (isValidJumpscareId(entry.getKey())) {
                    customSounds.add(entry.getKey());
                }
            }
        }

        if (!hideDefaultAudio || customSounds.isEmpty()) {
            soundSet.add("fakestep");
        }
        soundSet.addAll(customSounds);

        cachedAudioSounds.clear();
        cachedAudioSounds.addAll(soundSet);

        if (!cachedAudioSounds.isEmpty() && !soundSet.contains(selectedAudioSound.toLowerCase(Locale.ROOT))) {
            selectedAudioSound = cachedAudioSounds.get(0);
        }

        // ─── TAB BAR ───
        int tabY = cardTop + 24;
        int tabW = (cardWidth - 28) / 4;
        String[] tabLabels = {"VISUAL", "AUDIO", "CUSTOM", "KONTROL"};
        for (int i = 0; i < tabLabels.length; i++) {
            final int tabIdx = i;
            boolean active = (currentTab == i);
            String label = tabLabels[i];
            this.addRenderableWidget(createStyledButton(cardLeft + 14 + tabW * i, tabY, tabW - 2, 16, Component.literal(label), btn -> {
                this.currentTab = tabIdx;
                this.init();
            }, ButtonStyle.TAB, active));
        }

        int contentLeft = cardLeft + 14;
        int contentRight = cardLeft + cardWidth - 14;
        int contentWidth = contentRight - contentLeft;

        if (currentTab == 0) {
            buildVisualTab(cardLeft, cardTop, cardWidth, contentLeft, contentWidth);
        } else if (currentTab == 1) {
            buildAudioTab(cardLeft, cardTop, cardWidth, contentLeft, contentWidth);
        } else if (currentTab == 2) {
            buildCustomTab(cardLeft, cardTop, cardWidth, contentLeft, contentWidth);
        } else if (currentTab == 3) {
            buildControlTab(cardLeft, cardTop, cardWidth, contentLeft, contentWidth);
        }

        // ─── FOOTER ───
        int footerY = cardTop + cardHeight - 19;
        this.addRenderableWidget(createStyledButton(midX - 80, footerY, 160, 14, Component.literal("§7⟳ Reload Config & Assets"), btn -> {
            JumpscareAssetManager.getInstance().reloadAssets();
            this.statusMessage = "§a Config & assets reloaded";
            this.statusColor = 0x55FF55;
            this.init();
        }, ButtonStyle.DEFAULT, false));
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 0: VISUAL JUMPSCARE
    // ═══════════════════════════════════════════════════════════
    private void buildVisualTab(int cardLeft, int cardTop, int cardWidth, int contentLeft, int contentWidth) {
        int y = cardTop + 44;

        // --- SECTION 1: TARGET PLAYER ---
        int totalPlayerPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
        if (playerPage >= totalPlayerPages) playerPage = totalPlayerPages - 1;
        if (playerPage < 0) playerPage = 0;

        // "@a" toggle button
        boolean isAllSelected = assignments.containsKey("@a") || "@a".equalsIgnoreCase(selectedPlayer);
        String allLabel = (isAllSelected ? "§c✓ §f@a (Semua)" : "§7@a (Semua)");
        this.addRenderableWidget(createStyledButton(contentLeft + 80, y, 68, 12, Component.literal(allLabel), btn -> {
            if (assignments.containsKey("@a")) {
                assignments.remove("@a");
            } else {
                this.selectedPlayer = "@a";
                assignments.put("@a", selectedJumpscareId);
            }
            if (this.targetPlayerBox != null) {
                this.targetPlayerBox.setValue("@a");
            }
            this.init();
        }, ButtonStyle.CHIP, isAllSelected));

        // Player pagination buttons
        if (totalPlayerPages > 1) {
            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 46, y, 14, 12, Component.literal("◀"), btn -> {
                if (playerPage > 0) {
                    playerPage--;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));

            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 14, y, 14, 12, Component.literal("▶"), btn -> {
                if (playerPage < totalPlayerPages - 1) {
                    playerPage++;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));
        }

        // Online Players Cards Row (5 per page with 3D Head Avatars!)
        int pBtnY = y + 14;
        int pStart = playerPage * 5;
        int pEnd = Math.min(cachedOnlinePlayers.size(), pStart + 5);
        int pCount = pEnd - pStart;
        if (pCount > 0) {
            int gap = 3;
            int btnW = (contentWidth - ((pCount - 1) * gap)) / pCount;
            for (int i = 0; i < pCount; i++) {
                final String pName = cachedOnlinePlayers.get(pStart + i);
                boolean isAdded = assignments.containsKey(pName);
                boolean isSel = pName.equalsIgnoreCase(selectedPlayer);
                int btnX = contentLeft + i * (btnW + gap);

                this.addRenderableWidget(createPlayerCardButton(btnX, pBtnY, btnW, 15, pName, isAdded, isSel, btn -> {
                    this.selectedPlayer = pName;
                    if (!assignments.containsKey(pName)) {
                        assignments.put(pName, selectedJumpscareId);
                    }
                    if (this.targetPlayerBox != null) {
                        this.targetPlayerBox.setValue(pName);
                    }
                    this.init();
                }));
            }
        }

        // Target EditBox (offset right to leave room for live player head preview)
        String curTarget = this.targetPlayerBox != null ? this.targetPlayerBox.getValue() : selectedPlayer;
        this.targetPlayerBox = new EditBox(this.font, contentLeft + 20, y + 31, contentWidth - 20, 14, Component.literal("Target"));
        this.targetPlayerBox.setMaxLength(32);
        this.targetPlayerBox.setValue(curTarget);
        this.targetPlayerBox.setHint(Component.literal("Target player (@a / klik kartu di atas)..."));
        this.addRenderableWidget(this.targetPlayerBox);

        // --- SECTION 2: JUMPSCARE ID ---
        int idSectionY = y + 48;
        int totalIdPages = Math.max(1, (int) Math.ceil((double) cachedValidIds.size() / 5.0));
        if (idPage >= totalIdPages) idPage = totalIdPages - 1;
        if (idPage < 0) idPage = 0;

        if (totalIdPages > 1) {
            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 46, idSectionY, 14, 12, Component.literal("◀"), btn -> {
                if (idPage > 0) {
                    idPage--;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));

            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 14, idSectionY, 14, 12, Component.literal("▶"), btn -> {
                if (idPage < totalIdPages - 1) {
                    idPage++;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));
        }

        // ID Chips Row (5 per page)
        int idBtnY = idSectionY + 14;
        int idStart = idPage * 5;
        int idEnd = Math.min(cachedValidIds.size(), idStart + 5);
        int idDisplayCount = idEnd - idStart;
        if (idDisplayCount > 0) {
            int gap = 3;
            int btnW = (contentWidth - ((idDisplayCount - 1) * gap)) / idDisplayCount;
            for (int i = 0; i < idDisplayCount; i++) {
                final String idName = cachedValidIds.get(idStart + i);
                String currentAssigned = assignments.get(selectedPlayer);
                boolean isSelected = idName.equalsIgnoreCase(selectedJumpscareId) || idName.equalsIgnoreCase(currentAssigned);
                String label = (isSelected ? "§c» " : "👁 ") + truncate(idName, 8);
                int btnX = contentLeft + i * (btnW + gap);

                this.addRenderableWidget(createStyledButton(btnX, idBtnY, btnW, 14, Component.literal(label), btn -> {
                    this.selectedJumpscareId = idName;
                    String activeP = this.targetPlayerBox != null ? this.targetPlayerBox.getValue().trim() : selectedPlayer;
                    if (!activeP.isEmpty()) {
                        assignments.put(activeP, idName);
                    }
                    if (this.jumpscareIdBox != null) {
                        this.jumpscareIdBox.setValue(idName);
                    }
                    this.init();
                }, ButtonStyle.CHIP, isSelected));
            }
        }

        // ID EditBox (offset right for eye icon badge)
        String curId = this.jumpscareIdBox != null ? this.jumpscareIdBox.getValue() : selectedJumpscareId;
        this.jumpscareIdBox = new EditBox(this.font, contentLeft + 20, idSectionY + 31, contentWidth - 20, 14, Component.literal("ID"));
        this.jumpscareIdBox.setMaxLength(32);
        this.jumpscareIdBox.setValue(curId);
        this.jumpscareIdBox.setHint(Component.literal("ID jumpscare..."));
        this.addRenderableWidget(this.jumpscareIdBox);

        // --- SECTION 3: ASSIGNED TARGETS MANAGER ---
        int listSectionY = idSectionY + 48;
        List<Map.Entry<String, String>> entryList = new ArrayList<>(assignments.entrySet());
        int totalTargetPages = Math.max(1, (int) Math.ceil((double) entryList.size() / 4.0));
        if (targetListPage >= totalTargetPages) targetListPage = totalTargetPages - 1;
        if (targetListPage < 0) targetListPage = 0;

        // Clear All button
        if (!assignments.isEmpty()) {
            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 96, listSectionY, 44, 12, Component.literal("§c✕ Reset"), btn -> {
                assignments.clear();
                this.init();
            }, ButtonStyle.DANGER, false));
        }

        // Pagination for assigned targets list
        if (totalTargetPages > 1) {
            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 46, listSectionY, 14, 12, Component.literal("◀"), btn -> {
                if (targetListPage > 0) {
                    targetListPage--;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));

            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 14, listSectionY, 14, 12, Component.literal("▶"), btn -> {
                if (targetListPage < totalTargetPages - 1) {
                    targetListPage++;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));
        }

        // Remove buttons for visible assignments
        int tStart = targetListPage * 4;
        int tEnd = Math.min(entryList.size(), tStart + 4);
        int itemY = listSectionY + 13;
        for (int i = tStart; i < tEnd; i++) {
            final String tName = entryList.get(i).getKey();
            int rowY = itemY + ((i - tStart) * 12);
            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 16, rowY + 1, 16, 10, Component.literal("✕"), btn -> {
                assignments.remove(tName);
                this.init();
            }, ButtonStyle.DANGER, false));
        }

        // --- SECTION 4: ACTION BUTTONS ---
        int actionY = listSectionY + 62;
        int actBtnW1 = (int) (contentWidth * 0.48);
        int actBtnW2 = (int) (contentWidth * 0.24);
        int actBtnW3 = contentWidth - actBtnW1 - actBtnW2 - 8;

        this.addRenderableWidget(createStyledButton(contentLeft, actionY, actBtnW1, 16, Component.literal("§l☠ TEMBAK JUMPSCARE"), btn -> {
            String target = this.targetPlayerBox.getValue().trim();
            String id = this.jumpscareIdBox.getValue().trim();
            if (id.isEmpty()) id = "spooky";

            if (assignments.isEmpty() && !target.isEmpty()) {
                assignments.put(target, id);
            }

            if (assignments.isEmpty()) {
                this.statusMessage = "§c Pilih player target dulu!";
                this.statusColor = 0xFF5555;
            } else if (this.minecraft != null && this.minecraft.player != null) {
                JumpscareNetworking.sendBatchJumpscarePacket(assignments);
                this.statusMessage = "§a Jumpscare dikirim ke " + assignments.size() + " target";
                this.statusColor = 0x55FF55;
                this.onClose();
            }
        }, ButtonStyle.PRIMARY, false));

        this.addRenderableWidget(createStyledButton(contentLeft + actBtnW1 + 4, actionY, actBtnW2, 16, Component.literal("👁 Preview"), btn -> {
            String id = this.jumpscareIdBox.getValue().trim();
            if (id.isEmpty()) id = "spooky";

            String imgUrl = "";
            String soundUrl = "";
            JumpscareServerConfig.JumpscareEntry entry = JumpscareServerConfig.getEntry(id);
            if (entry != null) {
                if (entry.imageUrl != null) imgUrl = entry.imageUrl;
                if (entry.soundUrl != null) soundUrl = entry.soundUrl;
            }

            JumpscareAssetManager.getInstance().fetchAndTrigger(id, imgUrl, soundUrl, 2000, 1.2f);
            this.onClose();
        }, ButtonStyle.DEFAULT, false));

        this.addRenderableWidget(createStyledButton(contentLeft + actBtnW1 + actBtnW2 + 8, actionY, actBtnW3, 16, Component.literal("🗑 Hapus ID"), btn -> {
            String id = this.jumpscareIdBox.getValue().trim();
            if (!id.isEmpty() && !id.equalsIgnoreCase("spooky")) {
                JumpscareNetworking.sendRemoveJumpscarePacket(id);
                assignments.values().removeIf(v -> v.equalsIgnoreCase(id));
                this.statusMessage = "§a '" + id + "' dihapus dari server";
                this.statusColor = 0x55FF55;
                this.selectedJumpscareId = "spooky";
                this.init();
            } else {
                this.statusMessage = "§c ID 'spooky' tidak bisa dihapus";
                this.statusColor = 0xFF5555;
            }
        }, ButtonStyle.DANGER, false));

        // Ghost Attack Jumpscare Selection Row
        int ghostAtkY = actionY + 18;
        String curGhostId = this.jumpscareIdBox != null ? this.jumpscareIdBox.getValue().trim() : selectedJumpscareId;
        if (curGhostId.isEmpty()) curGhostId = activeGhostAttackId;
        final String targetGhostId = curGhostId;
        int btnW1 = (int) (contentWidth * 0.74);
        int btnW2 = contentWidth - btnW1 - 4;

        boolean isAlreadyActive = targetGhostId.equalsIgnoreCase(activeGhostAttackId);
        String label = isAlreadyActive
            ? "§a✔ Serangan Ghost Aktif: §f" + activeGhostAttackId
            : "§e⚡ Pasang Serangan Ghost: §f" + targetGhostId + " §7(Aktif: " + activeGhostAttackId + ")";

        this.addRenderableWidget(createStyledButton(contentLeft, ghostAtkY, btnW1, 15, Component.literal(label), btn -> {
            activeGhostAttackId = targetGhostId;
            selectedJumpscareId = targetGhostId;
            JumpscareNetworking.sendSetGhostAttackPacket(targetGhostId);
            this.statusMessage = "§a Serangan Ghost diatur: " + targetGhostId;
            this.statusColor = 0x55FF55;
            this.init();
        }, isAlreadyActive ? ButtonStyle.CHIP : ButtonStyle.DEFAULT, isAlreadyActive));

        boolean isRandomActive = "random".equalsIgnoreCase(activeGhostAttackId);
        String randomLabel = isRandomActive ? "§a✔ 🎲 Random" : "§8🎲 Random";
        this.addRenderableWidget(createStyledButton(contentLeft + btnW1 + 4, ghostAtkY, btnW2, 15, Component.literal(randomLabel), btn -> {
            activeGhostAttackId = "random";
            JumpscareNetworking.sendSetGhostAttackPacket("random");
            this.statusMessage = "§a Serangan Ghost diatur: RANDOM";
            this.statusColor = 0x55FF55;
            this.init();
        }, ButtonStyle.DEFAULT, isRandomActive));
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 1: AUDIO SCARE
    // ═══════════════════════════════════════════════════════════
    private void buildAudioTab(int cardLeft, int cardTop, int cardWidth, int contentLeft, int contentWidth) {
        int y = cardTop + 44;

        // --- SECTION 1: TARGET PLAYER ---
        int totalPlayerPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
        if (playerPage >= totalPlayerPages) playerPage = totalPlayerPages - 1;
        if (playerPage < 0) playerPage = 0;

        // "@a" toggle button
        boolean isAllSelected = "@a".equalsIgnoreCase(selectedPlayer) || (this.audioTargetBox != null && "@a".equalsIgnoreCase(this.audioTargetBox.getValue()));
        String allLabel = (isAllSelected ? "§c✓ §f@a (Semua)" : "§7@a (Semua)");
        this.addRenderableWidget(createStyledButton(contentLeft + 80, y, 68, 12, Component.literal(allLabel), btn -> {
            this.selectedPlayer = "@a";
            if (this.audioTargetBox != null) {
                this.audioTargetBox.setValue("@a");
            }
            this.init();
        }, ButtonStyle.CHIP, isAllSelected));

        // Player pagination buttons
        if (totalPlayerPages > 1) {
            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 46, y, 14, 12, Component.literal("◀"), btn -> {
                if (playerPage > 0) {
                    playerPage--;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));

            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 14, y, 14, 12, Component.literal("▶"), btn -> {
                if (playerPage < totalPlayerPages - 1) {
                    playerPage++;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));
        }

        // Online Players Cards Row (5 per page with 3D Head Avatars!)
        int pBtnY = y + 14;
        int pStart = playerPage * 5;
        int pEnd = Math.min(cachedOnlinePlayers.size(), pStart + 5);
        int pCount = pEnd - pStart;
        if (pCount > 0) {
            int gap = 3;
            int btnW = (contentWidth - ((pCount - 1) * gap)) / pCount;
            for (int i = 0; i < pCount; i++) {
                final String pName = cachedOnlinePlayers.get(pStart + i);
                boolean isSel = pName.equalsIgnoreCase(selectedPlayer);
                int btnX = contentLeft + i * (btnW + gap);

                this.addRenderableWidget(createPlayerCardButton(btnX, pBtnY, btnW, 15, pName, false, isSel, btn -> {
                    this.selectedPlayer = pName;
                    if (this.audioTargetBox != null) {
                        this.audioTargetBox.setValue(pName);
                    }
                    this.init();
                }));
            }
        }

        // Target EditBox
        String curTarget = this.audioTargetBox != null ? this.audioTargetBox.getValue() : selectedPlayer;
        this.audioTargetBox = new EditBox(this.font, contentLeft + 20, y + 31, contentWidth - 20, 14, Component.literal("Target"));
        this.audioTargetBox.setMaxLength(32);
        this.audioTargetBox.setValue(curTarget);
        this.audioTargetBox.setHint(Component.literal("Target player (@a / klik kartu di atas)..."));
        this.addRenderableWidget(this.audioTargetBox);

        // --- SECTION 2: SOUND PRESETS & SAVED AUDIO ---
        int soundSectionY = y + 48;
        int totalSoundPages = Math.max(1, (int) Math.ceil((double) cachedAudioSounds.size() / 5.0));
        if (audioSoundPage >= totalSoundPages) audioSoundPage = totalSoundPages - 1;
        if (audioSoundPage < 0) audioSoundPage = 0;

        if (totalSoundPages > 1) {
            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 46, soundSectionY, 14, 12, Component.literal("◀"), btn -> {
                if (audioSoundPage > 0) {
                    audioSoundPage--;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));

            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 14, soundSectionY, 14, 12, Component.literal("▶"), btn -> {
                if (audioSoundPage < totalSoundPages - 1) {
                    audioSoundPage++;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));
        }

        // Sound preset buttons row (5 per page)
        int sBtnY = soundSectionY + 14;
        int sStart = audioSoundPage * 5;
        int sEnd = Math.min(cachedAudioSounds.size(), sStart + 5);
        int sDisplayCount = sEnd - sStart;
        if (sDisplayCount > 0) {
            int gap = 3;
            int btnW = (contentWidth - ((sDisplayCount - 1) * gap)) / sDisplayCount;
            for (int i = 0; i < sDisplayCount; i++) {
                final String snd = cachedAudioSounds.get(sStart + i);
                boolean isSndSelected = snd.equalsIgnoreCase(selectedAudioSound);
                String label = (isSndSelected ? "§c» " : "🔊 ") + truncate(snd, 8);
                int btnX = contentLeft + i * (btnW + gap);

                this.addRenderableWidget(createStyledButton(btnX, sBtnY, btnW, 14, Component.literal(label), btn -> {
                    this.selectedAudioSound = snd;
                    if (this.audioSoundBox != null) {
                        this.audioSoundBox.setValue(snd);
                    }
                    String url = ClientConfigSync.getSoundUrl(snd);
                    if (url.isEmpty()) {
                        JumpscareServerConfig.JumpscareEntry entry = JumpscareServerConfig.getEntry(snd);
                        if (entry != null && entry.soundUrl != null) {
                            url = entry.soundUrl;
                        }
                    }
                    if (this.audioUrlBox != null) {
                        this.audioUrlBox.setValue(url);
                    }
                    this.init();
                }, ButtonStyle.CHIP, isSndSelected));
            }
        }

        // Sound ID EditBox
        String curSnd = this.audioSoundBox != null ? this.audioSoundBox.getValue() : selectedAudioSound;
        this.audioSoundBox = new EditBox(this.font, contentLeft + 20, soundSectionY + 31, contentWidth - 20, 14, Component.literal("Sound"));
        this.audioSoundBox.setMaxLength(64);
        this.audioSoundBox.setValue(curSnd);
        this.audioSoundBox.setHint(Component.literal("Sound ID / nama preset kustom"));
        this.addRenderableWidget(this.audioSoundBox);

        // Sound URL EditBox
        String curUrl = this.audioUrlBox != null ? this.audioUrlBox.getValue() : "";
        this.audioUrlBox = new EditBox(this.font, contentLeft + 20, soundSectionY + 47, contentWidth - 20, 14, Component.literal("URL"));
        this.audioUrlBox.setMaxLength(1024);
        this.audioUrlBox.setValue(curUrl);
        this.audioUrlBox.setHint(Component.literal("(Opsional) URL audio https://...mp3 / wav / ogg"));
        this.addRenderableWidget(this.audioUrlBox);

        // --- SECTION 3: MANAGE AUDIO (Paste, Simpan, Hapus) ---
        int manageY = soundSectionY + 64;
        int pasteW = 68;
        int hapusW = 62;
        int saveW = contentWidth - pasteW - hapusW - 8;

        this.addRenderableWidget(createStyledButton(contentLeft, manageY, pasteW, 15, Component.literal("§f📋 Paste"), btn -> {
            this.handleClipboardPaste();
        }, ButtonStyle.DEFAULT, false));

        this.addRenderableWidget(createStyledButton(contentLeft + pasteW + 4, manageY, saveW, 15, Component.literal("§a💾 Simpan Audio"), btn -> {
            String id = this.audioSoundBox != null ? this.audioSoundBox.getValue().trim() : "";
            String url = this.audioUrlBox != null ? this.audioUrlBox.getValue().trim() : "";

            if (id.isEmpty()) {
                this.statusMessage = "§c Masukkan Sound ID!";
                this.statusColor = 0xFF5555;
            } else if (!isValidJumpscareId(id)) {
                this.statusMessage = "§c ID tidak valid (minimal 2 huruf, bukan angka)";
                this.statusColor = 0xFF5555;
            } else {
                String existingImg = ClientConfigSync.getImageUrl(id);
                if (existingImg.isEmpty()) {
                    JumpscareServerConfig.JumpscareEntry existing = JumpscareServerConfig.getEntry(id);
                    if (existing != null && existing.imageUrl != null) {
                        existingImg = existing.imageUrl;
                    }
                }
                JumpscareServerConfig.addEntry(id, existingImg, url, 2000, 1.0f);
                JumpscareNetworking.sendAddJumpscarePacket(id, existingImg, url, 2000, 1.0f);
                if (!url.isEmpty()) {
                    JumpscareAssetManager.getInstance().preloadAsset(id, "", url);
                }
                this.statusMessage = "§a Audio '" + id + "' disimpan ke preset server!";
                this.statusColor = 0x55FF55;
                this.selectedAudioSound = id;
                this.init();
            }
        }, ButtonStyle.PRIMARY, false));

        this.addRenderableWidget(createStyledButton(contentLeft + pasteW + saveW + 8, manageY, hapusW, 15, Component.literal("§7🗑 Hapus"), btn -> {
            String id = this.audioSoundBox != null ? this.audioSoundBox.getValue().trim() : "";
            if (id.equalsIgnoreCase("fakestep")) {
                if (cachedAudioSounds.size() > 1) {
                    hideDefaultAudio = true;
                    this.statusMessage = "§a Suara bawaan 'fakestep' dihapus dari daftar!";
                    this.statusColor = 0x55FF55;
                    this.selectedAudioSound = "";
                    this.init();
                } else {
                    this.statusMessage = "§c Simpan minimal 1 audio kustom dulu sebelum menghapus bawaan";
                    this.statusColor = 0xFF5555;
                }
            } else if (!id.isEmpty()) {
                JumpscareNetworking.sendRemoveJumpscarePacket(id);
                JumpscareServerConfig.removeEntry(id);
                this.statusMessage = "§a Preset audio '" + id + "' dihapus";
                this.statusColor = 0x55FF55;
                this.selectedAudioSound = "fakestep";
                this.init();
            }
        }, ButtonStyle.DANGER, false));

        // --- SECTION 4: 3D DIRECTION TOGGLE ---
        int toggleY = manageY + 18;
        String toggleLabel = audioBehindPlayer
            ? "§a✔ §f3D Direction: Suara Di Belakang Target  §7(Positional 3D Audio)"
            : "§8✖ §73D Direction: Stereo Normal / Global";
        this.addRenderableWidget(createStyledButton(contentLeft, toggleY, contentWidth, 14, Component.literal(toggleLabel), btn -> {
            this.audioBehindPlayer = !this.audioBehindPlayer;
            this.init();
        }, ButtonStyle.CHIP, audioBehindPlayer));

        // --- SECTION 5: SHOOT & PREVIEW ACTIONS ---
        int actionY = toggleY + 17;
        int tembakW = (int) (contentWidth * 0.58);
        int prevW = contentWidth - tembakW - 4;

        this.addRenderableWidget(createStyledButton(contentLeft, actionY, tembakW, 16, Component.literal("§l☠ TEMBAK AUDIO"), btn -> {
            String target = this.audioTargetBox != null ? this.audioTargetBox.getValue().trim() : selectedPlayer;
            String sound = this.audioSoundBox != null ? this.audioSoundBox.getValue().trim() : selectedAudioSound;
            String url = this.audioUrlBox != null ? this.audioUrlBox.getValue().trim() : "";
            if (target.isEmpty()) target = "@a";
            if (sound.isEmpty()) sound = "fakestep";

            if (url.isEmpty()) {
                url = ClientConfigSync.getSoundUrl(sound);
                if (url.isEmpty()) {
                    JumpscareServerConfig.JumpscareEntry entry = JumpscareServerConfig.getEntry(sound);
                    if (entry != null && entry.soundUrl != null) {
                        url = entry.soundUrl;
                    }
                }
            }

            JumpscareNetworking.sendTriggerAudioPacket(target, sound, url, 1.0f, 1.0f, audioBehindPlayer);
            this.statusMessage = "§a Audio '" + sound + "' → " + target;
            this.statusColor = 0x55FF55;
            this.onClose();
        }, ButtonStyle.PRIMARY, false));

        this.addRenderableWidget(createStyledButton(contentLeft + tembakW + 4, actionY, prevW, 16, Component.literal("▶ Preview"), btn -> {
            String sound = this.audioSoundBox != null ? this.audioSoundBox.getValue().trim() : selectedAudioSound;
            String url = this.audioUrlBox != null ? this.audioUrlBox.getValue().trim() : "";
            if (sound.isEmpty()) sound = "fakestep";

            if (url.isEmpty()) {
                url = ClientConfigSync.getSoundUrl(sound);
                if (url.isEmpty()) {
                    JumpscareServerConfig.JumpscareEntry entry = JumpscareServerConfig.getEntry(sound);
                    if (entry != null && entry.soundUrl != null) {
                        url = entry.soundUrl;
                    }
                }
            }

            JumpscareAudioManager.playAudioScare(sound, url, 1.0f, 1.0f, audioBehindPlayer);
            this.statusMessage = "§a Memutar: '" + sound + "'";
            this.statusColor = 0x55FF55;
        }, ButtonStyle.DEFAULT, false));
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 2: TAMBAH CUSTOM
    // ═══════════════════════════════════════════════════════════
    private void buildCustomTab(int cardLeft, int cardTop, int cardWidth, int contentLeft, int contentWidth) {
        int y = cardTop + 44;

        // ID EditBox
        String curId = this.addIdBox != null ? this.addIdBox.getValue() : "";
        this.addIdBox = new EditBox(this.font, contentLeft + 20, y + 12, contentWidth - 20, 15, Component.literal("ID"));
        this.addIdBox.setMaxLength(32);
        this.addIdBox.setValue(curId);
        this.addIdBox.setHint(Component.literal("Nama ID baru (contoh: kunti, pocong)"));
        this.addRenderableWidget(this.addIdBox);

        // Image URL EditBox
        String curImg = this.addImgUrlBox != null ? this.addImgUrlBox.getValue() : "";
        this.addImgUrlBox = new EditBox(this.font, contentLeft + 20, y + 46, contentWidth - 20, 15, Component.literal("Image"));
        this.addImgUrlBox.setMaxLength(1024);
        this.addImgUrlBox.setValue(curImg);
        this.addImgUrlBox.setHint(Component.literal("https://...png / gif / jpg"));
        this.addRenderableWidget(this.addImgUrlBox);

        // Sound URL EditBox
        String curSnd = this.addSoundUrlBox != null ? this.addSoundUrlBox.getValue() : "";
        this.addSoundUrlBox = new EditBox(this.font, contentLeft + 20, y + 80, contentWidth - 20, 15, Component.literal("Sound"));
        this.addSoundUrlBox.setMaxLength(1024);
        this.addSoundUrlBox.setValue(curSnd);
        this.addSoundUrlBox.setHint(Component.literal("https://...wav / mp3 / ogg"));
        this.addRenderableWidget(this.addSoundUrlBox);

        // Action buttons
        int actionY = y + 108;
        int pasteW = 120;
        int saveW = contentWidth - pasteW - 4;

        this.addRenderableWidget(createStyledButton(contentLeft, actionY, pasteW, 16, Component.literal("§f📋 Paste Clipboard"), btn -> {
            this.handleClipboardPaste();
        }, ButtonStyle.DEFAULT, false));

        this.addRenderableWidget(createStyledButton(contentLeft + pasteW + 4, actionY, saveW, 16, Component.literal("§l💾 SIMPAN KE SERVER"), btn -> {
            String id = this.addIdBox.getValue().trim();
            String img = this.addImgUrlBox.getValue().trim();
            String sound = this.addSoundUrlBox.getValue().trim();

            if (id.isEmpty()) {
                this.statusMessage = "§c Nama ID tidak boleh kosong!";
                this.statusColor = 0xFF5555;
            } else if (!isValidJumpscareId(id)) {
                this.statusMessage = "§c ID tidak valid (minimal 2 huruf, bukan angka)";
                this.statusColor = 0xFF5555;
            } else {
                JumpscareServerConfig.addEntry(id, img, sound, 2500, 1.2f);
                JumpscareNetworking.sendAddJumpscarePacket(id, img, sound, 2500, 1.2f);
                JumpscareAssetManager.getInstance().preloadAsset(id, img, sound);
                this.statusMessage = "§a '" + id + "' disimpan!";
                this.statusColor = 0x55FF55;
                if (!img.isEmpty()) {
                    this.selectedJumpscareId = id;
                    this.currentTab = 0;
                } else {
                    this.selectedAudioSound = id;
                    this.currentTab = 1;
                }
                this.init();
            }
        }, ButtonStyle.PRIMARY, false));
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 3: MIND CONTROL
    // ═══════════════════════════════════════════════════════════
    private void buildControlTab(int cardLeft, int cardTop, int cardWidth, int contentLeft, int contentWidth) {
        int y = cardTop + 44;

        int totalPlayerPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
        if (playerPage >= totalPlayerPages) playerPage = totalPlayerPages - 1;
        if (playerPage < 0) playerPage = 0;

        // Pagination controls
        if (totalPlayerPages > 1) {
            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 46, y, 14, 12, Component.literal("◀"), btn -> {
                if (playerPage > 0) {
                    playerPage--;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));

            this.addRenderableWidget(createStyledButton(contentLeft + contentWidth - 14, y, 14, 12, Component.literal("▶"), btn -> {
                if (playerPage < totalPlayerPages - 1) {
                    playerPage++;
                    this.init();
                }
            }, ButtonStyle.DEFAULT, false));
        }

        // Online Players Cards Row (5 per page with 3D Head Avatars!)
        int pBtnY = y + 14;
        int pStart = playerPage * 5;
        int pEnd = Math.min(cachedOnlinePlayers.size(), pStart + 5);
        int pCount = pEnd - pStart;
        if (pCount > 0) {
            int gap = 3;
            int btnW = (contentWidth - ((pCount - 1) * gap)) / pCount;
            for (int i = 0; i < pCount; i++) {
                final String pName = cachedOnlinePlayers.get(pStart + i);
                boolean isSelected = pName.equalsIgnoreCase(selectedPlayer);
                int btnX = contentLeft + i * (btnW + gap);

                this.addRenderableWidget(createPlayerCardButton(btnX, pBtnY, btnW, 15, pName, false, isSelected, btn -> {
                    this.selectedPlayer = pName;
                    if (this.targetPlayerBox != null) {
                        this.targetPlayerBox.setValue(pName);
                    }
                    this.init();
                }));
            }
        }

        // Target EditBox
        String curTarget = this.targetPlayerBox != null ? this.targetPlayerBox.getValue() : selectedPlayer;
        this.targetPlayerBox = new EditBox(this.font, contentLeft + 20, y + 31, contentWidth - 20, 14, Component.literal("Target"));
        this.targetPlayerBox.setMaxLength(32);
        this.targetPlayerBox.setValue(curTarget);
        this.targetPlayerBox.setHint(Component.literal("Nama player target..."));
        this.addRenderableWidget(this.targetPlayerBox);

        // Action preset chips
        String[] actions = new String[]{"possess", "spin", "jump", "drop", "freeze", "look_down"};
        String[] icons = new String[]{"🌀 ", "🔄 ", "⬆ ", "📦 ", "❄ ", "⬇ "};
        int actBtnY = y + 58;
        int aGap = 3;
        int aBtnW = (contentWidth - ((actions.length - 1) * aGap)) / actions.length;
        for (int i = 0; i < actions.length; i++) {
            final String act = actions[i];
            boolean isSelected = act.equalsIgnoreCase(selectedControlAction);
            String label = icons[i] + act;
            int btnX = contentLeft + i * (aBtnW + aGap);

            this.addRenderableWidget(createStyledButton(btnX, actBtnY, aBtnW, 14, Component.literal(label), btn -> {
                this.selectedControlAction = act;
                if (this.controlActionBox != null) {
                    this.controlActionBox.setValue(act);
                }
                this.init();
            }, ButtonStyle.CHIP, isSelected));
        }

        // Action EditBox
        String curAct = this.controlActionBox != null ? this.controlActionBox.getValue() : selectedControlAction;
        this.controlActionBox = new EditBox(this.font, contentLeft + 20, y + 74, contentWidth - 20, 14, Component.literal("Action"));
        this.controlActionBox.setMaxLength(32);
        this.controlActionBox.setValue(curAct);
        this.controlActionBox.setHint(Component.literal("possess / spin / jump / drop / freeze"));
        this.addRenderableWidget(this.controlActionBox);

        // Execute button
        int actionY = y + 96;
        this.addRenderableWidget(createStyledButton(contentLeft, actionY, contentWidth, 16, Component.literal("§l💀 KENDALIKAN TUBUH"), btn -> {
            String target = this.targetPlayerBox.getValue().trim();
            String action = this.controlActionBox.getValue().trim();
            if (action.isEmpty()) action = "possess";

            if (target.isEmpty()) {
                this.statusMessage = "§c Masukkan nama player!";
                this.statusColor = 0xFF5555;
            } else if (this.minecraft != null && this.minecraft.player != null) {
                if (action.equalsIgnoreCase("possess")) {
                    PossessionControllerHandler.startPossession(target);
                    this.statusMessage = "§d Possessing '" + target + "'...";
                    this.statusColor = 0xFF55FF;
                    this.onClose();
                } else {
                    this.minecraft.player.connection.sendCommand("jumpscare control " + target + " " + action + " 3000");
                    this.statusMessage = "§d " + action + " → " + target;
                    this.statusColor = 0xFF55FF;
                    this.onClose();
                }
            }
        }, ButtonStyle.PRIMARY, false));

        // Ghost Team Buttons
        int ghostRowY = actionY + 19;
        int gBtnW = (contentWidth - 4) / 2;
        this.addRenderableWidget(createStyledButton(contentLeft, ghostRowY, gBtnW, 15, Component.literal("§7👻 + Team Ghost"), btn -> {
            String target = this.targetPlayerBox != null ? this.targetPlayerBox.getValue().trim() : selectedPlayer;
            if (target.isEmpty()) target = selectedPlayer;
            if (target.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                target = this.minecraft.player.getScoreboardName();
            }
            if (target.isEmpty()) {
                this.statusMessage = "§c Masukkan nama player!";
                this.statusColor = 0xFF5555;
            } else {
                JumpscareNetworking.sendGhostTeamPacket(target, "add");
                this.statusMessage = "§a Masuk Ghost: " + target;
                this.statusColor = 0x55FF55;
            }
        }, ButtonStyle.CHIP, false));

        this.addRenderableWidget(createStyledButton(contentLeft + gBtnW + 4, ghostRowY, contentWidth - gBtnW - 4, 15, Component.literal("§8❌ - Team Ghost"), btn -> {
            String target = this.targetPlayerBox != null ? this.targetPlayerBox.getValue().trim() : selectedPlayer;
            if (target.isEmpty()) target = selectedPlayer;
            if (target.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                target = this.minecraft.player.getScoreboardName();
            }
            if (target.isEmpty()) {
                this.statusMessage = "§c Masukkan nama player!";
                this.statusColor = 0xFF5555;
            } else {
                JumpscareNetworking.sendGhostTeamPacket(target, "remove");
                this.statusMessage = "§e Keluar Ghost: " + target;
                this.statusColor = 0xFFFF55;
            }
        }, ButtonStyle.DEFAULT, false));
    }

    // ═══════════════════════════════════════════════════════════
    // RENDER
    // ═══════════════════════════════════════════════════════════
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        // Dark atmospheric screen vignette
        g.fillGradient(0, 0, this.width, this.height / 3, 0x66250005, 0x00000000);
        g.fillGradient(0, this.height * 2 / 3, this.width, this.height, 0x00000000, 0x66250005);

        int cardWidth = Math.min(380, this.width - 16);
        int cardHeight = Math.min(270, this.height - 16);
        int midX = this.width / 2;
        int midY = this.height / 2;
        int cardLeft = midX - (cardWidth / 2);
        int cardTop = midY - (cardHeight / 2);
        int cardRight = cardLeft + cardWidth;
        int cardBottom = cardTop + cardHeight;

        // ─── OUTER SHADOW ───
        g.fill(cardLeft - 4, cardTop - 4, cardRight + 4, cardBottom + 4, 0x50000000);
        g.fill(cardLeft - 2, cardTop - 2, cardRight + 2, cardBottom + 2, 0x90000000);

        // ─── MAIN BACKGROUND TEXTURE ───
        ResourceLocation bg = JumpscareAssetManager.getInstance().getGuiBackgroundTexture();
        if (bg != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            g.blit(bg, cardLeft, cardTop, 0, 0, cardWidth, cardHeight, cardWidth, cardHeight);
        } else {
            g.fill(cardLeft, cardTop, cardRight, cardBottom, PANEL_BG);
        }

        // ─── HEADER BAR ───
        g.fill(cardLeft + 6, cardTop + 6, cardRight - 6, cardTop + 22, 0xDD090910);
        g.fill(cardLeft + 6, cardTop + 21, cardRight - 6, cardTop + 22, ACCENT_COLOR);

        // Title
        g.drawCenteredString(this.font, "§4§l☠ §c§lELESTIAL JUMPSCARE §4§l☠", midX, cardTop + 9, TEXT_WHITE);

        // ─── GLASSMORPHISM CONTENT AREA ───
        int contentLeft = cardLeft + 14;
        int contentRight = cardRight - 14;
        int contentWidth = contentRight - contentLeft;
        int contentTop = cardTop + 42;
        int contentBottom = cardBottom - 23;

        g.fill(contentLeft - 2, contentTop - 2, contentRight + 2, contentBottom + 2, 0xD40A0A12);
        g.fill(contentLeft - 2, contentTop - 2, contentRight + 2, contentTop - 1, 0x559B2335);
        g.fill(contentLeft - 2, contentBottom + 1, contentRight + 2, contentBottom + 2, 0x559B2335);
        g.fill(contentLeft - 2, contentTop - 2, contentLeft - 1, contentBottom + 2, 0x559B2335);
        g.fill(contentRight + 1, contentTop - 2, contentRight + 2, contentBottom + 2, 0x559B2335);

        // ─── TAB-SPECIFIC LABELS & AVATARS ───
        int y = cardTop + 44;
        if (currentTab == 0) {
            g.drawString(this.font, "§7Target Player", contentLeft + 2, y + 2, TEXT_GRAY, false);
            int totalPlayerPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
            if (totalPlayerPages > 1) {
                String pageText = "§7" + (playerPage + 1) + "§8/§7" + totalPlayerPages;
                g.drawCenteredString(this.font, pageText, contentLeft + contentWidth - 24, y + 2, TEXT_MUTED);
            }

            // Live Player Avatar Preview next to EditBox
            String curTarget = this.targetPlayerBox != null ? this.targetPlayerBox.getValue() : selectedPlayer;
            renderPlayerHead(g, curTarget, contentLeft + 2, y + 30, 16);

            int idSectionY = y + 48;
            g.drawString(this.font, "§7Jumpscare ID", contentLeft + 2, idSectionY + 2, TEXT_GRAY, false);
            int totalIdPages = Math.max(1, (int) Math.ceil((double) cachedValidIds.size() / 5.0));
            if (totalIdPages > 1) {
                String pageText = "§7" + (idPage + 1) + "§8/§7" + totalIdPages;
                g.drawCenteredString(this.font, pageText, contentLeft + contentWidth - 24, idSectionY + 2, TEXT_MUTED);
            }

            // Eye Icon next to ID EditBox
            g.fill(contentLeft + 2, idSectionY + 30, contentLeft + 18, idSectionY + 46, 0x991E1522);
            g.fill(contentLeft + 2, idSectionY + 30, contentLeft + 18, idSectionY + 31, 0x55FF2244);
            g.drawCenteredString(this.font, "👁", contentLeft + 10, idSectionY + 34, 0xFFFF4455);

            int listSectionY = idSectionY + 48;
            g.fill(contentLeft + 2, listSectionY - 2, contentRight - 2, listSectionY - 1, 0x309B2335);
            g.drawString(this.font, "§cTarget Ditugaskan §7(" + assignments.size() + ")", contentLeft + 2, listSectionY + 2, TEXT_GRAY, false);

            List<Map.Entry<String, String>> entryList = new ArrayList<>(assignments.entrySet());
            int totalTargetPages = Math.max(1, (int) Math.ceil((double) entryList.size() / 4.0));
            if (totalTargetPages > 1) {
                String pageText = "§7" + (targetListPage + 1) + "§8/§7" + totalTargetPages;
                g.drawCenteredString(this.font, pageText, contentLeft + contentWidth - 24, listSectionY + 2, TEXT_MUTED);
            }

            int itemY = listSectionY + 13;
            if (!entryList.isEmpty()) {
                int tStart = targetListPage * 4;
                int tEnd = Math.min(entryList.size(), tStart + 4);
                for (int i = tStart; i < tEnd; i++) {
                    Map.Entry<String, String> entry = entryList.get(i);
                    String tName = entry.getKey();
                    String tId = entry.getValue();
                    int rowY = itemY + ((i - tStart) * 12);
                    int rowH = 11;

                    // Row background
                    g.fill(contentLeft + 2, rowY, contentRight - 20, rowY + rowH, 0x66141420);
                    g.fill(contentLeft + 2, rowY, contentLeft + 3, rowY + rowH, 0xFFFF2244);

                    // Mini target player head avatar (9x9)
                    renderPlayerHead(g, tName, contentLeft + 5, rowY + 1, 9);

                    // Player Name
                    g.drawString(this.font, tName, contentLeft + 18, rowY + 2, 0xFFFFFFFF, false);

                    // Arrow
                    int nameW = this.font.width(tName);
                    g.drawString(this.font, "➔", contentLeft + 20 + nameW, rowY + 2, 0xFFFF3344, false);

                    // Jumpscare ID badge
                    int idX = contentLeft + 30 + nameW;
                    int idW = this.font.width(tId) + 6;
                    g.fill(idX, rowY + 1, idX + idW, rowY + 10, 0xCC400E1A);
                    g.fill(idX, rowY + 1, idX + idW, rowY + 2, 0x88FF3355);
                    g.drawString(this.font, tId, idX + 3, rowY + 2, 0xFFFFDDDD, false);
                }
            } else {
                g.drawString(this.font, "§8(Belum ada target - klik kartu player di atas atau ketik nama)", contentLeft + 4, itemY + 2, TEXT_MUTED, false);
            }
        } else if (currentTab == 1) {
            g.drawString(this.font, "§7Target Player", contentLeft + 2, y + 2, TEXT_GRAY, false);
            int totalPlayerPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
            if (totalPlayerPages > 1) {
                String pageText = "§7" + (playerPage + 1) + "§8/§7" + totalPlayerPages;
                g.drawCenteredString(this.font, pageText, contentLeft + contentWidth - 24, y + 2, TEXT_MUTED);
            }

            // Live Player Avatar Preview next to EditBox
            String curTarget = this.audioTargetBox != null ? this.audioTargetBox.getValue() : selectedPlayer;
            renderPlayerHead(g, curTarget, contentLeft + 2, y + 30, 16);

            int soundSectionY = y + 48;
            g.drawString(this.font, "§7Sound Preset & Kustom", contentLeft + 2, soundSectionY + 2, TEXT_GRAY, false);
            int totalSoundPages = Math.max(1, (int) Math.ceil((double) cachedAudioSounds.size() / 5.0));
            if (totalSoundPages > 1) {
                String pageText = "§7" + (audioSoundPage + 1) + "§8/§7" + totalSoundPages;
                g.drawCenteredString(this.font, pageText, contentLeft + contentWidth - 24, soundSectionY + 2, TEXT_MUTED);
            }

            // Sound icon next to sound ID Box
            g.fill(contentLeft + 2, soundSectionY + 30, contentLeft + 18, soundSectionY + 46, 0x991E1522);
            g.drawCenteredString(this.font, "🔊", contentLeft + 10, soundSectionY + 34, 0xFFFF4455);

            // Link icon next to URL Box
            g.fill(contentLeft + 2, soundSectionY + 46, contentLeft + 18, soundSectionY + 62, 0x991E1522);
            g.drawCenteredString(this.font, "🌐", contentLeft + 10, soundSectionY + 50, 0xFF88CCFF);
        } else if (currentTab == 2) {
            g.drawString(this.font, "§7Nama ID Baru", contentLeft + 2, y + 1, TEXT_GRAY, false);
            g.fill(contentLeft + 2, y + 12, contentLeft + 18, y + 28, 0x991E1522);
            g.drawCenteredString(this.font, "🏷", contentLeft + 10, y + 15, 0xFFFF4455);

            g.drawString(this.font, "§7Link Gambar §8(png / gif / jpg)", contentLeft + 2, y + 35, TEXT_GRAY, false);
            g.fill(contentLeft + 2, y + 46, contentLeft + 18, y + 62, 0x991E1522);
            g.drawCenteredString(this.font, "🖼", contentLeft + 10, y + 49, 0xFF88FF88);

            g.drawString(this.font, "§7Link Suara §8(wav / mp3 / ogg)", contentLeft + 2, y + 69, TEXT_GRAY, false);
            g.fill(contentLeft + 2, y + 80, contentLeft + 18, y + 96, 0x991E1522);
            g.drawCenteredString(this.font, "🎵", contentLeft + 10, y + 83, 0xFFFFCC44);
        } else if (currentTab == 3) {
            g.drawString(this.font, "§7Target Player", contentLeft + 2, y + 2, TEXT_GRAY, false);
            int totalPlayerPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
            if (totalPlayerPages > 1) {
                String pageText = "§7" + (playerPage + 1) + "§8/§7" + totalPlayerPages;
                g.drawCenteredString(this.font, pageText, contentLeft + contentWidth - 24, y + 2, TEXT_MUTED);
            }

            // Live Player Avatar Preview next to EditBox
            String curTarget = this.targetPlayerBox != null ? this.targetPlayerBox.getValue() : selectedPlayer;
            renderPlayerHead(g, curTarget, contentLeft + 2, y + 30, 16);

            g.drawString(this.font, "§7Aksi Kontrol Tubuh", contentLeft + 2, y + 48, TEXT_GRAY, false);
            g.fill(contentLeft + 2, y + 73, contentLeft + 18, y + 89, 0x991E1522);
            g.drawCenteredString(this.font, "⚡", contentLeft + 10, y + 76, 0xFFFF4455);
        }

        // ─── STATUS MESSAGE ───
        if (statusMessage != null && !statusMessage.isEmpty()) {
            g.drawCenteredString(this.font, statusMessage, midX, cardBottom + 4, statusColor);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    // ═══════════════════════════════════════════════════════════
    // MOUSE SCROLL SUPPORT
    // ═══════════════════════════════════════════════════════════
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int cardHeight = Math.min(270, this.height - 16);
        int midY = this.height / 2;
        int cardTop = midY - (cardHeight / 2);
        int dir = delta > 0 ? -1 : 1;

        if (currentTab == 0) {
            if (mouseY >= cardTop + 44 && mouseY <= cardTop + 88) {
                int totalPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
                int newPage = Math.max(0, Math.min(totalPages - 1, playerPage + dir));
                if (newPage != playerPage) {
                    playerPage = newPage;
                    this.init();
                    return true;
                }
            } else if (mouseY > cardTop + 88 && mouseY <= cardTop + 138) {
                int totalPages = Math.max(1, (int) Math.ceil((double) cachedValidIds.size() / 5.0));
                int newPage = Math.max(0, Math.min(totalPages - 1, idPage + dir));
                if (newPage != idPage) {
                    idPage = newPage;
                    this.init();
                    return true;
                }
            } else if (mouseY > cardTop + 138 && mouseY <= cardTop + 205) {
                int totalPages = Math.max(1, (int) Math.ceil((double) assignments.size() / 4.0));
                int newPage = Math.max(0, Math.min(totalPages - 1, targetListPage + dir));
                if (newPage != targetListPage) {
                    targetListPage = newPage;
                    this.init();
                    return true;
                }
            }
        } else if (currentTab == 1) {
            if (mouseY >= cardTop + 44 && mouseY <= cardTop + 88) {
                int totalPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
                int newPage = Math.max(0, Math.min(totalPages - 1, playerPage + dir));
                if (newPage != playerPage) {
                    playerPage = newPage;
                    this.init();
                    return true;
                }
            } else if (mouseY > cardTop + 88 && mouseY <= cardTop + 140) {
                int totalPages = Math.max(1, (int) Math.ceil((double) cachedAudioSounds.size() / 5.0));
                int newPage = Math.max(0, Math.min(totalPages - 1, audioSoundPage + dir));
                if (newPage != audioSoundPage) {
                    audioSoundPage = newPage;
                    this.init();
                    return true;
                }
            }
        } else if (currentTab == 3) {
            if (mouseY >= cardTop + 44 && mouseY <= cardTop + 88) {
                int totalPages = Math.max(1, (int) Math.ceil((double) cachedOnlinePlayers.size() / 5.0));
                int newPage = Math.max(0, Math.min(totalPages - 1, playerPage + dir));
                if (newPage != playerPage) {
                    playerPage = newPage;
                    this.init();
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ═══════════════════════════════════════════════════════════
    // CLIPBOARD PASTE HANDLER
    // ═══════════════════════════════════════════════════════════
    private void handleClipboardPaste() {
        if (this.minecraft == null || this.minecraft.keyboardHandler == null) return;

        try {
            String clip = this.minecraft.keyboardHandler.getClipboard().trim();
            if (clip.isEmpty()) {
                this.statusMessage = "§c Clipboard kosong!";
                this.statusColor = 0xFF5555;
                return;
            }

            if (this.currentTab == 1) {
                if (clip.startsWith("http://") || clip.startsWith("https://")) {
                    if (this.audioUrlBox != null) {
                        this.audioUrlBox.setValue(clip);
                        if (this.audioSoundBox != null && (this.audioSoundBox.getValue().isEmpty() || isDefaultPreset(this.audioSoundBox.getValue()))) {
                            String derivedName = extractNameFromUrl(clip);
                            if (isValidJumpscareId(derivedName)) {
                                this.audioSoundBox.setValue(derivedName);
                            }
                        }
                        this.statusMessage = "§a Link audio ditempel";
                        this.statusColor = 0x55FF55;
                    }
                } else {
                    if (this.audioSoundBox != null) {
                        this.audioSoundBox.setValue(clip);
                        this.statusMessage = "§a Sound ID ditempel";
                        this.statusColor = 0x55FF55;
                    }
                }
                return;
            }

            String[] parts = clip.split("\\s+");
            boolean pastedAny = false;

            for (String part : parts) {
                String lower = part.toLowerCase(Locale.ROOT);
                if (lower.startsWith("http://") || lower.startsWith("https://")) {
                    if (lower.contains(".png") || lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".gif") || lower.contains(".webp") || lower.contains("format=png") || lower.contains("format=gif")) {
                        if (this.addImgUrlBox != null) {
                            this.addImgUrlBox.setValue(part);
                            pastedAny = true;
                        }
                    } else if (lower.contains(".wav") || lower.contains(".mp3") || lower.contains(".ogg") || lower.contains(".flac") || lower.contains("format=mp3") || lower.contains("audio")) {
                        if (this.addSoundUrlBox != null) {
                            this.addSoundUrlBox.setValue(part);
                            pastedAny = true;
                        }
                    }
                }
            }

            if (!pastedAny) {
                if (this.addImgUrlBox != null && this.addImgUrlBox.getValue().isEmpty()) {
                    this.addImgUrlBox.setValue(clip);
                    pastedAny = true;
                } else if (this.addSoundUrlBox != null && this.addSoundUrlBox.getValue().isEmpty()) {
                    this.addSoundUrlBox.setValue(clip);
                    pastedAny = true;
                } else if (this.addImgUrlBox != null) {
                    this.addImgUrlBox.setValue(clip);
                    pastedAny = true;
                }
            }

            if (pastedAny) {
                this.statusMessage = "§a Clipboard ditempel";
                this.statusColor = 0x55FF55;
            }
        } catch (Exception e) {
            this.statusMessage = "§c Gagal baca clipboard";
            this.statusColor = 0xFF5555;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
