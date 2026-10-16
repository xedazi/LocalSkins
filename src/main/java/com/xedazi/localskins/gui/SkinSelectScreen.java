package com.xedazi.localskins.gui;

import com.xedazi.localskins.skin.LocalSkinManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SkinSelectScreen extends Screen {
    private final Screen parent;
    private final List<Path> skins = new ArrayList<>(), visibleSkins = new ArrayList<>();
    private int scroll = 0;
    private String status = "";
    private Path selectedFile;
    private TextFieldWidget searchField, urlField;
    private static final int ROW_HEIGHT = 22, LEFT_WIDTH = 310;
    public SkinSelectScreen(Screen parent) { super(new TranslatableText("screen.localskins.title")); this.parent = parent; }
    @Override protected void init() {
        int left = width / 2 - LEFT_WIDTH / 2;
        searchField = new TextFieldWidget(textRenderer, left, 43, LEFT_WIDTH, 18, new TranslatableText("screen.localskins.search"));
        searchField.setMaxLength(100); searchField.setSuggestion(new TranslatableText("screen.localskins.search_hint").getString());
        searchField.setChangedListener(v -> { scroll = 0; rebuildVisible(); }); addChild(searchField);
        urlField = new TextFieldWidget(textRenderer, left, 65, LEFT_WIDTH, 18, new TranslatableText("screen.localskins.png_url"));
        urlField.setMaxLength(500); urlField.setSuggestion(new TranslatableText("screen.localskins.png_url_hint").getString()); addChild(urlField);
        int bw=74,gap=4,row2=height-28,x=width/2-(bw*4+gap*3)/2;
        addButton(new ButtonWidget(x,row2,bw,20,new TranslatableText("screen.localskins.folder"),b->LocalSkinManager.openSkinDirectory()));
        addButton(new ButtonWidget(x+(bw+gap),row2,bw,20,new TranslatableText("screen.localskins.refresh"),b->refreshSkins()));
        addButton(new ButtonWidget(x+(bw+gap)*2,row2,bw,20,new TranslatableText("screen.localskins.clear"),b->{LocalSkinManager.clearSelection();status=new TranslatableText("screen.localskins.cleared").getString();}));
        addButton(new ButtonWidget(x+(bw+gap)*3,row2,bw,20,new TranslatableText("gui.done"),b->client.openScreen(parent)));
        int y=height-52;
        addButton(new ButtonWidget(width/2-154,y,100,20,new TranslatableText("screen.localskins.download"),b->downloadFromUrl()));
        addButton(new ButtonWidget(width/2-50,y,100,20,new TranslatableText("screen.localskins.namemc"),b->Util.getOperatingSystem().open("https://ru.namemc.com/")));
        addButton(new ButtonWidget(width/2+54,y,100,20,new TranslatableText("screen.localskins.refresh_list"),b->refreshSkins()));
        refreshSkins();
    }
    private int listTop(){return 91;}
    private int visibleRows(){return Math.max(1,(height-165)/ROW_HEIGHT);}
    private void rebuildVisible(){visibleSkins.clear();String q=searchField==null?"":searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);for(Path p:skins)if(p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).contains(q))visibleSkins.add(p);scroll=Math.max(0,Math.min(scroll,Math.max(0,visibleSkins.size()-visibleRows())));}
    private void refreshSkins(){skins.clear();skins.addAll(LocalSkinManager.listSkins());if(selectedFile!=null&&!java.nio.file.Files.exists(selectedFile))selectedFile=null;rebuildVisible();}
    private void downloadFromUrl(){String address=urlField.getText().trim();if(address.isEmpty()){status=new TranslatableText("screen.localskins.paste_url").getString();return;}status=new TranslatableText("screen.localskins.downloading").getString();Thread worker=new Thread(()->{try{Path downloaded=LocalSkinManager.downloadSkin(address);MinecraftClient.getInstance().execute(()->{refreshSkins();selectedFile=downloaded;boolean applied=LocalSkinManager.select(downloaded);urlField.setText("");status=new TranslatableText(applied?"screen.localskins.download_applied":"screen.localskins.downloaded",downloaded.getFileName().toString()).getString();});}catch(Exception e){MinecraftClient.getInstance().execute(()->status=new TranslatableText("screen.localskins.download_error",e.getMessage()==null?"?":e.getMessage()).getString());}},"LocalSkins-download");worker.setDaemon(true);worker.start();}
    @Override public void render(MatrixStack matrices,int mouseX,int mouseY,float delta){renderBackground(matrices);drawCenteredText(matrices,textRenderer,title,width/2,7,0xFFFFFF);drawCenteredText(matrices,textRenderer,new TranslatableText("screen.localskins.folder_path",LocalSkinManager.getSkinDirectory().toString()),width/2,24,0xAAAAAA);int left=width/2-LEFT_WIDTH/2,top=listTop();for(int row=0;row<visibleRows();row++){int index=scroll+row;if(index>=visibleSkins.size())break;Path skin=visibleSkins.get(index);int y=top+row*ROW_HEIGHT;boolean hovered=mouseX>=left&&mouseX<=left+LEFT_WIDTH&&mouseY>=y&&mouseY<y+19;fill(matrices,left,y,left+LEFT_WIDTH,y+19,skin.equals(selectedFile)?0xFF454545:(hovered?0xFF555555:0xFF333333));String name=skin.getFileName().toString();if(name.length()>31)name=name.substring(0,28)+"...";drawTextWithShadow(matrices,textRenderer,new LiteralText(name),left+7,y+5,0xFFFFFF);if(skin.getFileName().toString().equals(LocalSkinManager.getSelectedFileName()))drawTextWithShadow(matrices,textRenderer,new TranslatableText("screen.localskins.active"),left+215,y+5,0x55FF55);drawTextWithShadow(matrices,textRenderer,new LiteralText("✎"),left+286,y+4,0xFFFFFF);}if(visibleSkins.isEmpty())drawCenteredText(matrices,textRenderer,new TranslatableText("screen.localskins.empty"),width/2,top+12,0xFFAAAA);drawCenteredText(matrices,textRenderer,new LiteralText(status),width/2,height-66,0xFFFF55);super.render(matrices,mouseX,mouseY,delta);}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){if(searchField.mouseClicked(mouseX,mouseY,button)||urlField.mouseClicked(mouseX,mouseY,button))return true;if(button==0){int left=width/2-LEFT_WIDTH/2,top=listTop();if(mouseX>=left&&mouseX<=left+LEFT_WIDTH&&mouseY>=top){int row=((int)mouseY-top)/ROW_HEIGHT,index=scroll+row;if(row>=0&&row<visibleRows()&&index>=0&&index<visibleSkins.size()){Path skin=visibleSkins.get(index);if(mouseX>=left+270){client.openScreen(new RenameSkinScreen(this,skin));return true;}selectedFile=skin;if(LocalSkinManager.select(skin))status=new TranslatableText("screen.localskins.applied",skin.getFileName().toString()).getString();else status=new TranslatableText("screen.localskins.invalid_png").getString();return true;}}}return super.mouseClicked(mouseX,mouseY,button);}
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double amount){scroll=Math.max(0,Math.min(Math.max(0,visibleSkins.size()-visibleRows()),scroll-(int)Math.signum(amount)*2));return true;}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(keyCode==256){client.openScreen(parent);return true;}if(searchField.isFocused()||urlField.isFocused())return super.keyPressed(keyCode,scanCode,modifiers);if(keyCode==264){scroll=Math.min(Math.max(0,visibleSkins.size()-visibleRows()),scroll+1);return true;}if(keyCode==265){scroll=Math.max(0,scroll-1);return true;}return super.keyPressed(keyCode,scanCode,modifiers);}
}
