package ru.flashsafe.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.flashsafe.fs.FSObject;

public class DBManager {
	private static final String MYSQL_HOST = "eu-cdbr-azure-north-d.cloudapp.net";
	private static final String MYSQL_DB = "flashsaALXAwyvq4";
	private static final String MYSQL_USER = "b3bc423eeb16ef";
	private static final String MYSQL_PASS = "7ba0a76cb635087";
	private static Connection db = null;
	
	private static Connection getDB() {
		if(db == null) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
				db = DriverManager.getConnection("jdbc:mysql://" + MYSQL_HOST + "/" + MYSQL_DB, MYSQL_USER, MYSQL_PASS);
			} catch(SQLException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return db;
	}
	
	public static boolean checkAuth(String access_token) {
		boolean auth = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getDB().prepareStatement("SELECT `users`.`id`, `devices`.`device_id`, `users`.`name`, `users`.`lastname`, `users`.`total_size`, `users`.`used_size`, "
					+ "`devices`.`token`, `devices`.`timestamp` FROM `users` LEFT JOIN `devices` ON `devices`.`token`=? WHERE `users`.`id`=`devices`.`uid`");
			ps.setString(1, access_token);
			rs = ps.executeQuery();
			long timestamp = 0;
			if(rs.next()) {
				timestamp = Long.parseLong(rs.getString("timestamp"))*1000;
			}
			if(timestamp > 0 && (System.currentTimeMillis() - timestamp <= 15 * 60 * 1000))  { // Время жизни токена - 15 минут
		        auth = true;
		    }
		} catch(SQLException e) {
			e.printStackTrace();
		} finally {
			if(rs != null) try { rs.close(); } catch(SQLException e) { e.printStackTrace(); }
			if(ps != null) try { ps.close(); } catch(SQLException e) { e.printStackTrace(); }
		}
		return auth;
	}
	
	@SuppressWarnings("resource")
	public static HashMap<String, String> getAuthToken(int device_id, String ip_address) {
	  if(device_id > 0) {
		  PreparedStatement ps = null;
		  ResultSet rs = null;
		  try {
			  ps = getDB().prepareStatement("SELECT `version`, `secret`, `secret2` FROM `devices` WHERE `device_id`=?;");
			  ps.setInt(1, device_id);
			  rs = ps.executeQuery();
			  if(rs.next()) {
				  if(rs.getInt("version") == 1) {
					  long time = System.currentTimeMillis() / 1000;
					  String secret = md5(device_id + rs.getString("secret") + time);
					  ps = getDB().prepareStatement("INSERT INTO `auth` (`device_id`, `token1`, `token2`, `timestamp`, `ip`) VALUES (?, ?, ?, ?, ?);");
					  ps.setInt(1, device_id);
					  ps.setString(2, secret);
					  ps.setString(3, md5(secret + rs.getString("secret2") + time));
					  ps.setLong(4, time);
					  ps.setString(5, ip_address);
					  ps.execute();
					  HashMap<String, String> result = new HashMap<>();
					  result.put("timestamp", String.valueOf(time));
					  result.put("auth_token", secret);
					  return result;
				  }
			  }
		  } catch(SQLException e) {
			  e.printStackTrace();
		  } finally {
			  if(rs != null) try { rs.close(); } catch(SQLException e) { e.printStackTrace(); }
			  if(ps != null) try { ps.close(); } catch(SQLException e) { e.printStackTrace(); }
		  } 
	  }
	  return null;
	}
	
	@SuppressWarnings("resource")
	public static HashMap<String, String> getAccessToken(int device_id, String auth_token, String ip_address) {
	  if(device_id > 0) {
		  PreparedStatement ps = null;
		  ResultSet rs = null;
		  try {
			  ps = getDB().prepareStatement("SELECT `timestamp` FROM `auth` WHERE `device_id`=? AND `token2`=? AND `ip`=?;");
			  ps.setInt(1, device_id);
			  ps.setString(2, auth_token);
			  ps.setString(3, ip_address);
			  rs = ps.executeQuery();
			  if(rs.next()) {
				  long time = System.currentTimeMillis() / 1000;
				  ps = getDB().prepareStatement("UPDATE `devices` SET `token`=?, `timestamp`=? WHERE `device_id`=? LIMIT 1;");
				  ps.setString(1, auth_token);
				  ps.setString(2, String.valueOf(time));
				  ps.setInt(3, device_id);
				  ps.executeUpdate();
				  ps = getDB().prepareStatement("DELETE FROM `auth` WHERE `token2`=?;");
				  ps.setString(1,  auth_token);
				  ps.executeUpdate();
				  HashMap<String, String> result = new HashMap<>();
				  result.put("timestamp", String.valueOf(time));
				  result.put("access_token", auth_token);
				  result.put("timeout", "900");
				  return result;
			  }
		  } catch(SQLException e) {
			  e.printStackTrace();
		  } finally {
			  if(rs != null) try { rs.close(); } catch(SQLException e) { e.printStackTrace(); }
			  if(ps != null) try { ps.close(); } catch(SQLException e) { e.printStackTrace(); }
		  } 
	  }
	  return null;
	}
	
	public static List<FSObject> getFoldersList(int dir_id) {
	  PreparedStatement ps = null;
	  ResultSet rs = null;
	  try {
		  if(dir_id == -1) {
			  ps = getDB().prepareStatement("SELECT `id`, `device`, `type`, `name`, `format`, `size`, `pincode`, `count`, `create_time`, `update_time` "
	                   + "FROM `files` WHERE (`uid`=? AND `deleted`=1);");
			  ps.setInt(1, 1);
		  } else {
			  ps = getDB().prepareStatement("SELECT `id`, `device`, `type`, `name`, `format`, `size`, `pincode`, `count`, `create_time`, `update_time` "
	                   + "FROM `files` WHERE (`uid`=? AND `parent`=? AND `deleted`=0);");
			  ps.setInt(1, 1);
			  ps.setInt(2, dir_id);
		  }
		  rs = ps.executeQuery();
		  //if(rs.getFetchSize() > 0) { - wrong usage! see: https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getFetchSize()
            List<FSObject> items = new ArrayList<>();
            while (rs.next()) {
                FSObject item = new FSObject(rs.getLong("id"), rs.getString("type"), rs.getString("name"),
                        rs.getString("format"), rs.getLong("size"), rs.getString("pincode"), rs.getLong("count"),
                        rs.getString("create_time"), rs.getString("update_time"));
                items.add(item);
            }
            return items.isEmpty() ? null : items;
		  /*} else {
			  return null;
		  }*/
	  } catch(SQLException e) {
		  e.printStackTrace();
		  return null;
	  } finally {
		  if(rs != null) try { rs.close(); } catch(SQLException e) { e.printStackTrace(); }
		  if(ps != null) try { ps.close(); } catch(SQLException e) { e.printStackTrace(); }
	  }  
	}
	
	@SuppressWarnings("resource")
	public static int createDir(int parent, String name) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getDB().prepareStatement("INSERT INTO `files` (`id`, `uid`, `device`, `parent`, `type`, `name`, `format`, `size`, `pincode`, `hidden`, `count`, `create_time`, `update_time`) VALUES (NULL, ?, '0', ?, 'dir', ?, '', '0', '', '0', '0', ?, ?);");
			ps.setInt(1, 1);
			ps.setInt(2, parent);
			ps.setString(3, name);
			String time = String.valueOf(System.currentTimeMillis() / 1000);
			ps.setString(4, time);
			ps.setString(5, time);
			ps.executeUpdate();
			ps = getDB().prepareStatement("SELECT LAST_INSERT_ID();");
			rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1);
		} catch(SQLException e) {
			e.printStackTrace();
			return 0;
		} finally {
			if(rs != null) try { rs.close(); } catch(SQLException e) { e.printStackTrace(); }
			if(ps != null) try { ps.close(); } catch(SQLException e) { e.printStackTrace(); }
		} 
	}
	
	@SuppressWarnings("resource")
	public static String delete(int parent, int item_id) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getDB().prepareStatement("SELECT `id`, `device`, `type`, `name`, `format`, `size`, `pincode`, `count`, `create_time`, `update_time` FROM `files` WHERE (`uid`=? AND `id`=? AND `parent`=?);");
			ps.setInt(1, 1);
			ps.setInt(2, item_id);
			ps.setInt(3, parent);
			rs = ps.executeQuery();
			if(rs.next()) {
				String type = rs.getString("type");
				String name = rs.getString("name");
				if(type.equals("file")) {
					ps = getDB().prepareStatement("DELETE FROM `files` WHERE (`uid`=? AND `id`=? AND `parent`=?);");
					ps.setInt(1, 1);
					ps.setInt(2, item_id);
					ps.setInt(3, parent);
					ps.executeUpdate();
					return name;
					// TODO: need delete a physical file
				} else if(type.equals("dir")) {
					ps = getDB().prepareStatement("SELECT `id`, `device`, `type`, `name`, `format`, `size`, `pincode`, `count`, `create_time`, `update_time` FROM `files` WHERE (`uid`=? AND `type`='dir' AND `id`=?);");
					ps.setInt(1, 1);
					ps.setInt(2, item_id);
					rs = ps.executeQuery();
					if(!rs.next()) {
						ps = getDB().prepareStatement("DELETE FROM `files` WHERE (`uid`=? AND `id`=? AND `parent`=?);");
						ps.setInt(1, 1);
						ps.setInt(2, item_id);
						ps.setInt(3, parent);
						ps.executeUpdate();
						return name;
						// TODO: need delete a physical directory with all sub-directories and files
					}
				}
			} else {
				return null;
			}
		} catch(SQLException e) {
			e.printStackTrace();
		} finally {
			if(rs != null) try { rs.close(); } catch(SQLException e) { e.printStackTrace(); }
			if(ps != null) try { ps.close(); } catch(SQLException e) { e.printStackTrace(); }
		}
		return null; 
	}
	
	@SuppressWarnings("resource")
	public static int addFile(int dir_id, String name, String type, long size) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getDB().prepareStatement("INSERT INTO `files` (`id`, `uid`, `device`, `parent`, `type`, `name`, `format`, `size`, `pincode`, `hidden`, `count`, `create_time`, `update_time`) "
					+ "VALUES (NULL, ?, '0', ?, 'file', ?, ?, ?, '', 0, 0, ?, ?);");
			ps.setInt(1, 1);
			ps.setInt(2, dir_id);
			ps.setString(3, name);
			ps.setString(4, type);
			ps.setLong(5, size);
			String time = String.valueOf(System.currentTimeMillis() / 1000);
			ps.setString(6, time);
			ps.setString(7, time);
			ps.executeUpdate();
			ps = getDB().prepareStatement("UPDATE `users` SET `count_files`=`count_files`+1, `used_size`=`used_size`+? WHERE `id`=?;");
			ps.setLong(1, size);
			ps.setInt(2, 1);
			ps.executeUpdate();
			ps = getDB().prepareStatement("SELECT LAST_INSERT_ID();");
			rs = ps.executeQuery();
			rs.next();
			int file_id = rs.getInt(1);
			if(dir_id > 0) {
				ps = getDB().prepareStatement("UPDATE `files` SET `count`+=1, `size`+=? WHERE (`id`=? AND `type`='dir') LIMIT 1;");
				ps.setLong(1, size);
				ps.setInt(2, dir_id);
			}
			return file_id;
		} catch(SQLException e) {
			e.printStackTrace();
			return 0;
		} finally {
			if(rs != null) try { rs.close(); } catch(SQLException e) { e.printStackTrace(); }
			if(ps != null) try { ps.close(); } catch(SQLException e) { e.printStackTrace(); }
		} 
	}
	
	public static String getFileNameByID(int file_id) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getDB().prepareStatement("SELECT `name` FROM `files` WHERE `id`=?;");
			ps.setInt(1, file_id);
			rs = ps.executeQuery();
			rs.next();
			return rs.getString("name");
		} catch(SQLException e) {
			e.printStackTrace();
			return null;
		} finally {
			if(rs != null) try { rs.close(); } catch(SQLException e) { e.printStackTrace(); }
			if(ps != null) try { ps.close(); } catch(SQLException e) { e.printStackTrace(); }
		} 
	}
	
	private static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Error on generate MD5", nsae);
        }
    }

}
