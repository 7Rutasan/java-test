import java.sql.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class MusicLibrary {
    private static final String AUDIO_FOLDER = "AudioDB";
    private static final String DB_URL = "jdbc:postgresql://localhost:5434/music";
    private static final String USER = "postgres";
    private static final String PASS = "Maqsat07112005.";
    private User currentUser = null;

    public static String getDbUrl() {
        return DB_URL;
    }

    public static String getUser() {
        return USER;
    }

    public static String getPass() {
        return PASS;
    }

    public static void main(String[] args) {
        new MusicLibrary().run();
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            int choice;
            do {
                displayMenu();
                System.out.print("Выберите действие: ");
                while (!scanner.hasNextInt()) {
                    System.out.println("Некорректный ввод. Пожалуйста, введите целое число.");
                    scanner.next();
                }
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                handleUserChoice(scanner, choice);
            } while (choice != 0);
        }
    }

    private void displayMenu() {
        if (currentUser == null) {
            System.out.println("Главный экран:");
            System.out.println("1. Зарегистрировать пользователя");
            System.out.println("2. Войти в аккаунт");
            System.out.println("0. Выйти");
        } else {
            currentUser.displayMenu();
        }
    }

    private void handleUserChoice(Scanner scanner, int choice) {
        if (currentUser == null) {
            switch (choice) {
                case 1:
                    registerUser(scanner);
                    break;
                case 2:
                    loginUser(scanner);
                    break;
                case 0:
                    System.out.println("До свидания!");
                    break;
                default:
                    System.out.println("Неверный выбор. Попробуйте еще раз.");
            }
        } else {
            currentUser.handleChoice(scanner, choice, this);
        }
    }

    private void registerUser(Scanner scanner) {
        System.out.print("Введите имя пользователя: ");
        String username = scanner.nextLine();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();

        try (Connection connection = DriverManager.getConnection(MusicLibrary.getDbUrl(), MusicLibrary.getUser(), MusicLibrary.getPass());
             PreparedStatement ps = connection.prepareStatement("INSERT INTO users (username, password, is_admin) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setBoolean(3, false); // by default, user is not an admin
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        currentUser = UserFactory.createUser(userId, username, password, false);
                        System.out.println("Пользователь зарегистрирован успешно!");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при регистрации пользователя.");
            e.printStackTrace();
        }
    }

    private void loginUser(Scanner scanner) {
        System.out.print("Введите имя пользователя: ");
        String username = scanner.nextLine();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();

        try (Connection connection = DriverManager.getConnection(MusicLibrary.getDbUrl(), MusicLibrary.getUser(), MusicLibrary.getPass());
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString("password").equals(password)) {
                    int userId = rs.getInt("user_id");
                    boolean isAdmin = rs.getBoolean("is_admin");
                    currentUser = UserFactory.createUser(userId, username, password, isAdmin);
                    System.out.println("Вход выполнен успешно!");
                } else {
                    System.out.println("Неверное имя пользователя или пароль.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при входе в аккаунт.");
            e.printStackTrace();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}

abstract class User {
    protected int userId;
    protected String username;
    protected String password;
    protected boolean isAdmin;

    public User(int userId, String username, String password, boolean isAdmin) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.isAdmin = isAdmin;
    }

    abstract void displayMenu();
    abstract void handleChoice(Scanner scanner, int choice, MusicLibrary library);
}

class UserFactory {
    public static User createUser(int userId, String username, String password, boolean isAdmin) {
        if (isAdmin) {
            return new AdminUser(userId, username, password);
        } else {
            return new CommonUser(userId, username, password);
        }
    }
}

class AdminUser extends User {
    public AdminUser(int userId, String username, String password) {
        super(userId, username, password, true);
    }

    @Override
    void displayMenu() {
        System.out.println("Админ-меню:");
        System.out.println("1. Показать всех пользователей");
        System.out.println("2. Удалить пользователя");
        System.out.println("9. Выйти с аккаунта");
        System.out.println("0. Выйти");
    }

    @Override
    void handleChoice(Scanner scanner, int choice, MusicLibrary library) {
        switch (choice) {
            case 1:
                showAllUsers();
                break;
            case 2:
                deleteUser(scanner);
                break;
            case 9:
                library.setCurrentUser(null);
                System.out.println("Выход выполнен успешно!");
                break;
            case 0:
                System.out.println("До свидания!");
                System.exit(0);
                break;
            default:
                System.out.println("Неверный выбор. Попробуйте еще раз.");
        }
    }

    private void showAllUsers() {
        try (Connection connection = DriverManager.getConnection(MusicLibrary.getDbUrl(), MusicLibrary.getUser(), MusicLibrary.getPass());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT user_id, username FROM users")) {
            while (rs.next()) {
                int id = rs.getInt("user_id");
                String username = rs.getString("username");
                System.out.println("ID: " + id + ", Имя пользователя: " + username);
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при показе списка пользователей.");
            e.printStackTrace();
        }
    }

    private void deleteUser(Scanner scanner) {
        System.out.print("Введите ID пользователя для удаления: ");
        int userIdToDelete = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        try (Connection connection = DriverManager.getConnection(MusicLibrary.getDbUrl(), MusicLibrary.getUser(), MusicLibrary.getPass());
             PreparedStatement ps = connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userIdToDelete);
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Пользователь удален успешно.");
            } else {
                System.out.println("Пользователь с указанным ID не найден.");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при удалении пользователя.");
            e.printStackTrace();
        }
    }
}


class CommonUser extends User {
    public CommonUser(int userId, String username, String password) {
        super(userId, username, password, false);
    }

    @Override
    void displayMenu() {
        System.out.println("Меню пользователя:");
        System.out.println("1. Список треков");
        System.out.println("2. Добавить музыку");
        System.out.println("3. Установить музыку");
        System.out.println("4. Мой профиль");
        System.out.println("5. Изменить имя пользователя");
        System.out.println("6. Получить права админа");
        System.out.println("9. Выйти с аккаунта");
        System.out.println("0. Выйти");
    }

    @Override
    void handleChoice(Scanner scanner, int choice, MusicLibrary library) {
        switch (choice) {
            case 1:
                // Предположим, здесь логика для показа списка треков
                break;
            case 2:
                // Логика для добавления музыки
                break;
            case 3:
                // Логика для установки музыки
                break;
            case 4:
                showProfile();
                break;
            case 5:
                // Логика для изменения имени пользователя
                break;
            case 6:
                getAdminRights(scanner, library);
                break;
            case 9:
                library.setCurrentUser(null);
                System.out.println("Выход выполнен успешно!");
                break;
            case 0:
                System.out.println("До свидания!");
                System.exit(0);
                break;
            default:
                System.out.println("Неверный выбор. Попробуйте еще раз.");
        }
    }

    private void showProfile() {
        System.out.println("Имя пользователя: " + username);
        System.out.println("ID пользователя: " + userId);
    }

    private void getAdminRights(Scanner scanner, MusicLibrary library) {
        System.out.print("Введите пароль для подтверждения прав админа: ");
        String password = scanner.nextLine();
        if ("admin123".equals(password)) {
            library.setCurrentUser(new AdminUser(userId, username, this.password));
            System.out.println("Права администратора получены успешно.");
        } else {
            System.out.println("Неверный пароль. Права админа не предоставлены.");
        }
    }
}
