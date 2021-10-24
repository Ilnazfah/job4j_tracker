package ru.job4j.tracker;

import ru.job4j.tracker.model.Item;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SqlTracker implements Store {
    private Connection cn;

    public void init() {
        try (InputStream in = SqlTracker.class.getClassLoader().getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(in);
            Class.forName(config.getProperty("driver-class-name"));
            cn = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (cn != null) {
            cn.close();
        }
    }

    @Override
    public Item add(Item item) {
        Timestamp timestampFromLDT = Timestamp.valueOf(item.getCreated());
        try (PreparedStatement ps = cn.prepareStatement("insert into items(name, created) values (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setTimestamp(2, timestampFromLDT);
            ps.execute();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return item;
    }

    @Override
    public boolean replace(int id, Item item) {
        boolean res = false;
        Timestamp timestampFromLDT = Timestamp.valueOf(item.getCreated());
        try (PreparedStatement ps = cn.prepareStatement("update items set name = ?, created = ? where id = ?")) {
            ps.setString(1, item.getName());
            ps.setTimestamp(2, timestampFromLDT);
            ps.setInt(3, id);
            res = ps.executeUpdate() > 0;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return res;
    }

    @Override
    public boolean delete(int id) {
        boolean res = false;
        try (PreparedStatement ps = cn.prepareStatement("DELETE FROM items WHERE id = ?")) {
            ps.setInt(1, id);
            res = ps.executeUpdate() > 0;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return res;
    }

    @Override
    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement("select * from items")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime localDateTime = rs.getTimestamp("created").toLocalDateTime();
                    items.add(new Item(rs.getInt("id"),
                            rs.getString("name"),
                            localDateTime));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return items;
    }

    @Override
    public List<Item> findByName(String key) {
        List<Item> items = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement("select * from items where name = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime localDateTime = rs.getTimestamp("created").toLocalDateTime();
                    items.add(new Item(rs.getInt("id"),
                            rs.getString("name"),
                            localDateTime));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return items;
    }

    @Override
    public Item findById(int id) {
        Item res = null;
        try (PreparedStatement ps = cn.prepareStatement("select * from items where id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime localDateTime = rs.getTimestamp("created").toLocalDateTime();
                    res = new Item(rs.getInt("id"),
                            rs.getString("name"),
                            localDateTime);
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return res;
    }
}