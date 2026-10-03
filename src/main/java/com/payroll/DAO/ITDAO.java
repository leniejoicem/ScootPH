package com.payroll.DAO;

import com.payroll.domain.IT;
import com.payroll.domain.Person;
import com.payroll.subdomain.UserRole;
import com.payroll.validation.Password;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ITDAO {
    private Connection connection;
    private HRDAO hrService;

    public ITDAO(Connection connection) {
        this.connection = connection;
        this.hrService = new HRDAO(connection);
    }

    public IT getAccountById(int accountId) {
        IT account = null;
        String sql = "SELECT * FROM employee_account WHERE account_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                account = new IT();
                account.setAccountID(rs.getInt("account_id"));
                account.setEmpUserName(rs.getString("username"));
                account.setEmpPassword(rs.getString("password"));
                account.setTfa(rs.getString("tfa"));

                int roleID = rs.getInt("role_id");
                UserRole role = getByRolesId(roleID);
                account.setUserRole(role);

                int empID = rs.getInt("employee_id");
                Person empDetails = hrService.getByEmpID(empID);
                account.setEmpDetails(empDetails);
                account.setEmpID(empID);
            }

            rs.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving account by ID: " + e.getMessage());
        }

        return account;
    }

    public IT getUserAccount(String username, String plainPassword) {
        IT employeeAccount = null;

        if (username == null || username.trim().isEmpty() ||
            plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }

        if (connection != null) {
            String query = "SELECT * FROM employee_account WHERE lower(username) = lower(?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    String hashedPassword = resultSet.getString("password");

                    if (Password.verifyPassword(plainPassword, hashedPassword)) {
                        employeeAccount = new IT();
                        employeeAccount.setAccountID(resultSet.getInt("account_id"));
                        employeeAccount.setEmpUserName(resultSet.getString("username"));
                        employeeAccount.setEmpPassword(hashedPassword);
                        employeeAccount.setTfa(resultSet.getString("tfa"));

                        int roleID = resultSet.getInt("role_id");
                        UserRole role = getByRolesId(roleID);
                        employeeAccount.setUserRole(role);

                        int empID = resultSet.getInt("employee_id");
                        Person employeeDetails = hrService.getByEmpID(empID);
                        employeeAccount.setEmpDetails(employeeDetails);
                        employeeAccount.setEmpID(empID);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Database error during authentication: " + e.getMessage());
            }
        }
        return employeeAccount;
    }

    public record Credentials(int accountId, String username, String passwordHash) {
    }

    public Credentials findCredentials(String username) {
        String sql = "SELECT account_id, username, password FROM public.employee_account WHERE lower(username) = lower(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Credentials(rs.getInt(1), rs.getString(2), rs.getString(3)) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("findCredentials failed", e);
        }
    }

    public void storePasswordHash(int accountId, String passwordHash) {
        String sql = "UPDATE public.employee_account SET password = ? WHERE account_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("storePasswordHash failed", e);
        }
    }

    public void updateUsername(int empID, String username) {
        String sql = "UPDATE public.employee_account SET username = ? WHERE employee_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, empID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("updateUsername failed", e);
        }
    }

    public boolean isUsernameTaken(String username, Integer exceptEmployeeId) {
        String sql = "SELECT 1 FROM public.employee_account WHERE lower(username) = lower(?) "
                + "AND (? IS NULL OR employee_id <> ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            if (exceptEmployeeId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, exceptEmployeeId);
                ps.setInt(3, exceptEmployeeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("isUsernameTaken failed", e);
        }
    }

    public record AccountRow(int employeeId, String lastName, String firstName, String position,
            Integer accountId, String username, Integer roleId, String role, boolean twoFactorEnabled) {
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : (int) value;
    }

    public List<AccountRow> getAccountDirectory() {
        String sql = """
            SELECT e.employee_id, e.lastname, e.firstname, p.position,
                   a.account_id, a.username, r.id AS role_id, r.role,
                   (a.tfa IS NOT NULL AND a.tfa <> '') AS tfa_enabled
              FROM public.employee e
              LEFT JOIN public.employee_position p ON p.id = e.position
              LEFT JOIN public.employee_account a ON a.employee_id = e.employee_id
              LEFT JOIN public.user_roles r ON r.id = a.role_id
             ORDER BY e.employee_id
        """;
        List<AccountRow> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new AccountRow(rs.getInt("employee_id"), rs.getString("lastname"),
                        rs.getString("firstname"), rs.getString("position"),
                        nullableInt(rs, "account_id"), rs.getString("username"),
                        nullableInt(rs, "role_id"), rs.getString("role"),
                        rs.getBoolean("tfa_enabled")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("getAccountDirectory failed", e);
        }
        return rows;
    }

    public List<IT> getAllUserAccount() {
        List<IT> userAccounts = new ArrayList<>();

        if (connection != null) {
            String query = "SELECT * FROM employee_account";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    IT employeeAccount = new IT();
                    employeeAccount.setAccountID(resultSet.getInt("account_id"));
                    employeeAccount.setEmpUserName(resultSet.getString("username"));
                    employeeAccount.setEmpPassword(resultSet.getString("password"));

                    int roleID = resultSet.getInt("role_id");
                    UserRole role = getByRolesId(roleID);
                    employeeAccount.setUserRole(role);

                    int empID = resultSet.getInt("employee_id");
                    Person employeeDetails = hrService.getByEmpID(empID);
                    employeeAccount.setEmpDetails(employeeDetails);
                    employeeAccount.setEmpID(empID);

                    userAccounts.add(employeeAccount);
                }
            } catch (SQLException e) {
                System.err.println("Error retrieving all user accounts: " + e.getMessage());
            }
        }

        return userAccounts;
    }

    public boolean updateTfaSecret(int accountId, String secret) {
        String sql = "UPDATE employee_account SET tfa = ? WHERE account_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, secret);
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("updateTfaSecret failed", e);
        }
    }

    public IT getByEmpID(int empID) {
        IT employeeAccount = null;
        if (connection != null) {
            String query = "SELECT * FROM public.employee_account WHERE employee_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, empID);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    employeeAccount = new IT();
                    employeeAccount.setAccountID(resultSet.getInt("account_id"));
                    employeeAccount.setEmpUserName(resultSet.getString("username"));
                    employeeAccount.setEmpPassword(resultSet.getString("password"));

                    int roleID = resultSet.getInt("role_id");
                    UserRole role = getByRolesId(roleID);
                    employeeAccount.setUserRole(role);

                    Person employeeDetails = hrService.getByEmpID(empID);
                    employeeAccount.setEmpDetails(employeeDetails);
                    employeeAccount.setEmpID(empID);
                }

                resultSet.close();
            } catch (SQLException e) {
                System.err.println("Error retrieving account by employee ID: " + e.getMessage());
            }
        }
        return employeeAccount;
    }

    public void updateEmployeeCredentials(IT empAccount) {
        if (connection != null) {
            String query = "UPDATE public.employee_account SET username = ?, password = ? WHERE employee_id = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                String rawPassword = empAccount.getEmpPassword();

                String hashedPassword = Password.hashPassword(rawPassword);

                preparedStatement.setString(1, empAccount.getEmpUserName());
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.setInt(3, empAccount.getEmpID());

                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new DataAccessException("updateEmployeeCredentials failed", e);
            }
        } else {
            System.err.println("Database connection is null. Cannot update credentials.");
        }
    }

    public List<String> getMissingRolesAfterUpdate(int empId, String newRoleName) {
        List<String> requiredRoles = Arrays.asList("HR", "Finance", "IT");
        Map<String, Integer> roleCounts = new HashMap<>();

        String query = "SELECT role, assigned_count FROM vw_role_assignment_count";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                String role = resultSet.getString("role");
                int count = resultSet.getInt("assigned_count");
                roleCounts.put(role, count);
            }

            String currentRoleQuery = "SELECT role FROM vw_employee_roles WHERE employee_id = ?";

            try (PreparedStatement roleStmt = connection.prepareStatement(currentRoleQuery)) {
                roleStmt.setInt(1, empId);
                ResultSet roleRs = roleStmt.executeQuery();

                if (roleRs.next()) {
                    String currentRole = roleRs.getString("role");

                    if (!newRoleName.equalsIgnoreCase(currentRole)) {
                        roleCounts.put(currentRole, roleCounts.getOrDefault(currentRole, 0) - 1);
                        roleCounts.put(newRoleName, roleCounts.getOrDefault(newRoleName, 0) + 1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting missing roles: " + e.getMessage());
        }

        List<String> missingRoles = new ArrayList<>();
        for (String role : requiredRoles) {
            if (roleCounts.getOrDefault(role, 0) < 1) {
                missingRoles.add(role);
            }
        }

        return missingRoles;
    }

    public void updateEmployeeAccountWithRole(IT empAccount) {
        if (connection == null) {
            throw new IllegalStateException("Database connection is not established.");
        }

        Integer roleId = (empAccount.getUserRole() != null) ? empAccount.getUserRole().getId() : null;
        String sql = "CALL update_employee_account_with_role(?, ?, ?, ?)";

        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setString(1, empAccount.getEmpUserName());
            stmt.setString(2, empAccount.getEmpPassword());

            if (roleId != null) {
                stmt.setInt(3, roleId);
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }

            stmt.setInt(4, empAccount.getEmpID());
            stmt.execute();
        } catch (SQLException e) {
            throw new DataAccessException("updateEmployeeAccountWithRole failed", e);
        }
    }

    public List<UserRole> getAllUserRole() {
        List<UserRole> userRoles = new ArrayList<>();
        if (connection != null) {
            String query = "SELECT * FROM public.user_roles";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    UserRole userRole = new UserRole();
                    userRole.setId(resultSet.getInt("id"));
                    userRole.setRole(resultSet.getString("role"));
                    userRoles.add(userRole);
                }
            } catch (SQLException e) {
                System.err.println("Error retrieving user roles: " + e.getMessage());
            }
        }
        return userRoles;
    }

    public UserRole getByRolesId(int id) {
        UserRole userRole = null;
        if (connection != null) {
            String query = "SELECT * FROM public.user_roles WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, id);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    userRole = new UserRole();
                    userRole.setId(resultSet.getInt("id"));
                    userRole.setRole(resultSet.getString("role"));
                }

                resultSet.close();
            } catch (SQLException e) {
                System.err.println("Error retrieving role by ID: " + e.getMessage());
            }
        }
        return userRole;
    }

    public int verifyIdentity(String username, String birthday, String phoneNumber, String tin) {
        String sql = "SELECT a.account_id " +
                     "FROM employee_account a JOIN employee e ON a.employee_id = e.employee_id " +
                     "WHERE a.username = ? AND e.birthday = ? AND e.phone_number = ? AND e.tin = ?";

        if (connection != null) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setDate(2, java.sql.Date.valueOf(birthday));
                stmt.setString(3, phoneNumber);
                stmt.setString(4, tin);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("account_id");
                }
                rs.close();
            } catch (SQLException ex) {
                System.err.println("Error verifying identity: " + ex.getMessage());
            }
        }

        return -1;
    }

    public boolean updatePassword(int accountId, String newPassword) {
        String sql = "UPDATE public.employee_account SET password = ? WHERE account_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String hashedPassword = Password.hashPassword(newPassword);

            stmt.setString(1, hashedPassword);
            stmt.setInt(2, accountId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("updatePassword failed", e);
        }
    }

    public Person saveUserAccount(IT empAccount, Person empDetails) throws SQLException {
        if (connection == null) {
            throw new IllegalStateException("Database connection is not established.");
        }

        String sql = "CALL save_user_account(?, ?, ?, ?)";

        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setInt(1, empDetails.getEmpID());
            stmt.setString(2, empAccount.getEmpUserName());

            String hashedPassword = Password.hashPassword(empAccount.getEmpPassword());
            stmt.setString(3, hashedPassword);

            stmt.registerOutParameter(4, java.sql.Types.INTEGER);
            stmt.execute();

            int accountId = stmt.getInt(4);
            empAccount.setAccountID(accountId);
        } catch (SQLException e) {
            System.err.println("Error calling save_user_account: " + e.getMessage());
            throw e;
        }

        return empDetails;
    }

    public void deleteEmpAccount(int empID) {
        if (connection != null) {
            String sql = "CALL delete_emp_account(?)";
            try (CallableStatement stmt = connection.prepareCall(sql)) {
                stmt.setInt(1, empID);
                stmt.execute();
            } catch (SQLException e) {
                throw new DataAccessException("deleteEmpAccount failed", e);
            }
        }
    }
}
