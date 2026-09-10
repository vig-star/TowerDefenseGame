import com.example.judy.TowerDefenseApplication;
import com.example.judy.modules.Cannon;
import com.example.judy.modules.Crossbow;
import com.example.judy.modules.GameAdmin;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;

/**
 * Standalone smoke check using the application's actual JavaFX event handlers.
 * Renders the game scene directly; it does not capture the desktop or use a Robot.
 * Run through capture-gameplay.sh. This helper is separate from the original tests.
 */
public class TowerDefenseSmokeSnapshot {
    private static final int ACTION_TIMEOUT_SECONDS = 60;
    private static Stage gameStage;

    @FunctionalInterface
    interface Checked {
        void run() throws Exception;
    }

    static void fx(Checked action) throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                result.complete(null);
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        result.get(ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
        System.out.println("PASS " + message);
    }

    static Node node(Scene scene, String selector) {
        Node result = scene.lookup(selector);
        if (result == null) {
            throw new AssertionError("Missing node " + selector);
        }
        return result;
    }

    static Button textButton(Parent root, String text) {
        for (Node node : root.lookupAll(".button")) {
            if (node instanceof Button && ((Button) node).getText().equals(text)) {
                return (Button) node;
            }
        }
        throw new AssertionError("Missing button " + text);
    }

    static Stage stage(String title) {
        for (Window window : new ArrayList<>(Window.getWindows())) {
            if (window instanceof Stage && ((Stage) window).getTitle().equals(title)) {
                return (Stage) window;
            }
        }
        throw new AssertionError("Missing stage " + title);
    }

    static void mouseClick(Node target) {
        target.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 1, 1, 1, 1,
                MouseButton.PRIMARY, 1, false, false, false, false, false,
                false, false, false, false, false, new PickResult(target, 1, 1)));
    }

    static void buy(String type) throws Exception {
        fx(() -> ((Button) node(gameStage.getScene(), "#towerMenu")).fire());
        CompletableFuture<Void> purchase = new CompletableFuture<>();
        // The confirmation uses showAndWait, so respond through its nested event loop.
        Platform.runLater(() -> {
            try {
                mouseClick(node(stage("Tower Menu").getScene(), "#" + type));
                purchase.complete(null);
            } catch (Throwable failure) {
                purchase.completeExceptionally(failure);
            }
        });
        fx(() -> textButton(stage("Purchase Confirmation").getScene().getRoot(), "YES").fire());
        purchase.get(ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    static void place(String type, String tile) throws Exception {
        fx(() -> ((Button) node(gameStage.getScene(), "#inventoryMenu")).fire());
        fx(() -> mouseClick(node(stage("Inventory").getScene(), "#" + type)));
        fx(() -> ((Button) node(gameStage.getScene(), "#" + tile)).fire());
    }

    static void waitFor(BooleanSupplier condition, long millis, String description) throws Exception {
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (System.nanoTime() < until) {
            CompletableFuture<Boolean> value = new CompletableFuture<>();
            Platform.runLater(() -> value.complete(condition.getAsBoolean()));
            if (value.get(ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                System.out.println("PASS " + description);
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError(description + " timed out");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the output PNG path");
        }
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "smoke-timeout");
            thread.setDaemon(true);
            return thread;
        });
        watchdog.schedule(() -> {
            System.err.println("FAIL JavaFX smoke check exceeded 180 seconds");
            System.exit(1);
        }, 180, TimeUnit.SECONDS);
        int exitCode = 1;
        try {
            Platform.startup(() -> { });
            Platform.setImplicitExit(false);
            fx(() -> {
                gameStage = new Stage();
                new TowerDefenseApplication().start(gameStage);
            });
            fx(() -> {
                check(node(gameStage.getScene(), "#welcomeText") != null, "welcome screen loads");
                ((Button) node(gameStage.getScene(), "#start")).fire();
                ((TextField) node(gameStage.getScene(), "#nameInput")).setText("GitHub Demo");
                gameStage.getScene().getRoot().applyCss();
                textButton(gameStage.getScene().getRoot(), "Enter").fire();
                ((Button) node(gameStage.getScene(), "#easyButton")).fire();
                textButton(gameStage.getScene().getRoot(), "Start").fire();
                check(GameAdmin.getGame().getPlayer().getName().equals("GitHub Demo"),
                        "configuration stores player name");
                check(GameAdmin.getGame().getPlayer().getMoney() == 200,
                        "easy starts with $200");
                check(GameAdmin.getGame().getMonument().getHealth() == 150,
                        "easy monument starts with 150 health");
            });
            buy("cannon");
            buy("crossbow");
            fx(() -> {
                check(GameAdmin.getGame().getPlayer().getMoney() == 75,
                        "real purchase dialogs deduct $125");
                check(GameAdmin.getGame().getTowers().get(Cannon.NAME) == 1,
                        "cannon inventory increases");
                check(GameAdmin.getGame().getTowers().get(Crossbow.NAME) == 1,
                        "crossbow inventory increases");
            });
            place("cannon", "11");
            place("crossbow", "17");
            fx(() -> {
                check(GameAdmin.getGame().getTowersPlaced().size() == 2,
                        "inventory selection and valid tiles place two towers");
                check(GameAdmin.getGame().getTowers().get(Cannon.NAME) == 0,
                        "placement consumes cannon inventory");
                ((Button) node(gameStage.getScene(), "#startCombat")).fire();
            });
            waitFor(() -> GameAdmin.getGame().getDmgDealt() > 0, 8000,
                    "wave movement triggers tower combat");
            fx(() -> {
                Scene scene = gameStage.getScene();
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                WritableImage snapshot = scene.snapshot(null);
                int width = (int) snapshot.getWidth();
                int height = (int) snapshot.getHeight();
                BufferedImage png = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        png.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
                    }
                }
                File output = new File(args[0]).getAbsoluteFile();
                output.getParentFile().mkdirs();
                ImageIO.write(png, "png", output);
                System.out.println("SAVED " + output.getAbsolutePath() + " " + width + "x" + height);
                System.out.println("STATE money=" + GameAdmin.getGame().getPlayer().getMoney()
                        + " enemyHP=" + GameAdmin.getGame().getEnemy().get(0).getHealth()
                        + " damage=" + GameAdmin.getGame().getDmgDealt());
                for (Window window : new ArrayList<>(Window.getWindows())) {
                    window.hide();
                }
            });
            exitCode = 0;
        } catch (Exception | AssertionError failure) {
            failure.printStackTrace();
        } finally {
            Platform.exit();
            watchdog.shutdownNow();
            // End this standalone process, including the game's background tasks.
            System.exit(exitCode);
        }
    }
}
