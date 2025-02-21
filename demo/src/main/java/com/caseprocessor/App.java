package com.caseprocessor;

import com.caseprocessor.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.InputStream;

public class App extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // 加载应用程序图标
            InputStream iconStream = getClass().getResourceAsStream("/icons/app-icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
            
            // 创建主窗口
            MainWindow mainWindow = new MainWindow();
            mainWindow.start(primaryStage);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("file.encoding", "UTF-8");
        
        // 启动应用程序
        launch(args);
    }
}