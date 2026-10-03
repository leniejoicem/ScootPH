package com.payroll.DAO;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.payroll.domain.Person;
import com.payroll.domain.Employee;
import com.payroll.domain.HR.LeaveStatus;
import com.payroll.domain.IT;
import com.payroll.subdomain.EmployeePosition;
import com.payroll.subdomain.EmployeeStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;
import java.sql.Types;
import javax.swing.JComboBox;
public class HRDAO {
    private static final Logger LOGGER = Logger.getLogger(HRDAO.class.getName());

    private Connection connection;

    public HRDAO(Connection connection) {
        this.connection = connection;
    }

    public Person getByEmpID(int empID){
        return getByEmpID(empID,true);
    }

    public Person getByEmpID(int empID, boolean fetchSupervisor){
        Person employeeDetails = null ;
        if (connection != null) {
            String Query = "SELECT * FROM employee where employee_id = ?";
            ResultSet resultSet = null;
            PreparedStatement preparedStatement =null;
            try {
                preparedStatement = connection.prepareStatement(Query);
                preparedStatement.setInt(1,empID);
                resultSet = preparedStatement.executeQuery();
                if(resultSet.next()){
                    employeeDetails = toEmployeeDetails(resultSet, fetchSupervisor);
                }
                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
            }
        }
        return employeeDetails;
    }

    public EmployeePosition getPositionById(int id){
        EmployeePosition empPosition = null ;
        if (connection != null) {
            String Query = "SELECT * FROM public.employee_position where id = ?";
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(Query);
                preparedStatement.setInt(1,id);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    empPosition = new EmployeePosition();
                    empPosition.setId(resultSet.getInt("id"));
                    empPosition.setPosition(resultSet.getString("position"));
                }
                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
            }
        }
        return empPosition;
    }

    public void updateEmployeeCredentials(IT empAccount){
        if (connection == null) return;

        String call = "CALL update_employee_credentials(?, ?, ?)";

        try (CallableStatement stmt = connection.prepareCall(call)) {
            stmt.setString(1, empAccount.getEmpUserName());
            stmt.setString(2, empAccount.getEmpPassword());
            stmt.setInt(3, empAccount.getEmpID());

            stmt.execute();

            System.out.println("Credentials updated successfully.");
        } catch (SQLException e) {
            throw new DataAccessException("updateEmployeeCredentials failed", e);
        }
    }

    public EmployeeStatus getStatusById(int id){
        EmployeeStatus empStatus = null ;
        if (connection != null) {
            String Query = "SELECT * FROM public.employee_status where id = ?";
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(Query);
                preparedStatement.setInt(1,id);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    empStatus = new EmployeeStatus();
                    empStatus.setId(resultSet.getInt("id"));
                    empStatus.setStatus(resultSet.getString("status"));
                }
                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
            }
        }
        return empStatus;
    }

    public EmployeePosition loadPositionData(JComboBox<String> positionComboBox){
        return null;
    }

    public Person updateEmployeeDetails(Person empDetails){
        if (connection == null) return null;

        String call = "CALL update_employee_details(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        java.sql.Date birthDate = empDetails.getEmpBirthday() != null
            ? new java.sql.Date(empDetails.getEmpBirthday().getTime())
            : null;

        Integer supervisorId = empDetails.getEmpImmediateSupervisor() != null
            ? empDetails.getEmpImmediateSupervisor().getEmpID()
            : null;

        Integer positionId = empDetails.getEmpPosition() != null
            ? empDetails.getEmpPosition().getId()
            : null;

        Integer statusId = empDetails.getEmpStatus() != null
            ? empDetails.getEmpStatus().getId()
            : null;

        try (CallableStatement stmt = connection.prepareCall(call)) {

            stmt.setString(1, empDetails.getLastName());
            stmt.setString(2, empDetails.getFirstName());
            stmt.setDate(3, birthDate);
            stmt.setString(4, empDetails.getEmpAddress());
            stmt.setString(5, empDetails.getEmpPhoneNumber());
            stmt.setString(6, empDetails.getEmpSSS());
            stmt.setLong(7, empDetails.getEmpPhilHealth());
            stmt.setString(8, empDetails.getEmpTIN());
            stmt.setLong(9, empDetails.getEmpPagibig());

            if (statusId != null) {
                stmt.setInt(10, statusId);
            } else {
                stmt.setNull(10, java.sql.Types.INTEGER);
            }

            if (positionId != null) {
                stmt.setInt(11, positionId);
            } else {
                stmt.setNull(11, java.sql.Types.INTEGER);
            }

            if (supervisorId != null) {
                stmt.setInt(12, supervisorId);
            } else {
                stmt.setNull(12, java.sql.Types.INTEGER);
            }

            stmt.setBigDecimal(13, BigDecimal.valueOf(empDetails.getEmpBasicSalary()).setScale(2, RoundingMode.HALF_UP));
            stmt.setBigDecimal(14, BigDecimal.valueOf(empDetails.getEmpRice()).setScale(2, RoundingMode.HALF_UP));
            stmt.setBigDecimal(15, BigDecimal.valueOf(empDetails.getEmpPhone()).setScale(2, RoundingMode.HALF_UP));
            stmt.setBigDecimal(16, BigDecimal.valueOf(empDetails.getEmpClothing()).setScale(2, RoundingMode.HALF_UP));
            stmt.setBigDecimal(17, BigDecimal.valueOf(empDetails.getEmpMonthlyRate()).setScale(2, RoundingMode.HALF_UP));
            stmt.setBigDecimal(18, BigDecimal.valueOf(empDetails.getEmpHourlyRate()).setScale(2, RoundingMode.HALF_UP));
            stmt.setInt(19, empDetails.getEmpID());

            stmt.execute();
            System.out.println("Update successful.");
        } catch (SQLException e) {
            throw new DataAccessException("updateEmployeeDetails failed", e);
        }

        return empDetails;
    }

    public boolean isDuplicateEmployee(Person empDetails) {
        String query = """
            SELECT employee_id FROM public.employee
            WHERE lastname IS NOT DISTINCT FROM ? AND firstname IS NOT DISTINCT FROM ?
            AND birthday IS NOT DISTINCT FROM ? AND address IS NOT DISTINCT FROM ?
            AND phone_number IS NOT DISTINCT FROM ? AND sss IS NOT DISTINCT FROM ?
            AND philhealth IS NOT DISTINCT FROM ? AND tin IS NOT DISTINCT FROM ?
            AND pag_ibig IS NOT DISTINCT FROM ? AND status IS NOT DISTINCT FROM ?
            AND position IS NOT DISTINCT FROM ? AND immediate_supervisor IS NOT DISTINCT FROM ?
        """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, empDetails.getLastName());
            preparedStatement.setString(2, empDetails.getFirstName());
            preparedStatement.setDate(3, empDetails.getEmpBirthday() == null ? null
                    : new java.sql.Date(empDetails.getEmpBirthday().getTime()));
            preparedStatement.setString(4, empDetails.getEmpAddress());
            preparedStatement.setString(5, empDetails.getEmpPhoneNumber());
            preparedStatement.setString(6, empDetails.getEmpSSS());
            preparedStatement.setLong(7, empDetails.getEmpPhilHealth());
            preparedStatement.setString(8, empDetails.getEmpTIN());
            preparedStatement.setLong(9, empDetails.getEmpPagibig());
            if (empDetails.getEmpStatus() != null) {
                preparedStatement.setObject(10, empDetails.getEmpStatus().getId(), Types.INTEGER);
            } else {
                preparedStatement.setNull(10, Types.INTEGER);
            }

            if (empDetails.getEmpPosition() != null) {
                preparedStatement.setObject(11, empDetails.getEmpPosition().getId(), Types.INTEGER);
            } else {
                preparedStatement.setNull(11, Types.INTEGER);
            }

            if (empDetails.getEmpImmediateSupervisor() != null) {
                preparedStatement.setObject(12, empDetails.getEmpImmediateSupervisor().getEmpID(), Types.INTEGER);
            } else {
                preparedStatement.setNull(12, Types.INTEGER);
            }

            try (ResultSet resultset = preparedStatement.executeQuery()) {
                return resultset.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error", e);
        }

        return false;
    }

    public Integer getEmployeeIdIfExists(Person empDetails) {
        String query = """
            SELECT employee_id FROM public.employee
            WHERE lastname = ? AND firstname = ? AND birthday = ? AND address = ? AND phone_number = ?
            AND sss = ? AND philhealth = ? AND tin = ? AND pag_ibig = ? AND status = ? AND position = ? AND immediate_supervisor = ?
        """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, empDetails.getLastName());
            preparedStatement.setString(2, empDetails.getFirstName());
            preparedStatement.setDate(3, new java.sql.Date(empDetails.getEmpBirthday().getTime()));
            preparedStatement.setString(4, empDetails.getEmpAddress());
            preparedStatement.setString(5, empDetails.getEmpPhoneNumber());
            preparedStatement.setString(6, empDetails.getEmpSSS());
            preparedStatement.setLong(7, empDetails.getEmpPhilHealth());
            preparedStatement.setString(8, empDetails.getEmpTIN());
            preparedStatement.setLong(9, empDetails.getEmpPagibig());
            preparedStatement.setObject(10, empDetails.getEmpStatus().getId(), Types.INTEGER);
            preparedStatement.setObject(11, empDetails.getEmpPosition().getId(), Types.INTEGER);
            preparedStatement.setObject(12, empDetails.getEmpImmediateSupervisor().getEmpID(), Types.INTEGER);

            try (ResultSet resultset = preparedStatement.executeQuery()) {
                if (resultset.next()) {
                    return resultset.getInt("employee_id");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error", e);
        }

        return null;
    }

    public Person addEmployeeDetails(Person empDetails) {
        if (connection == null) return null;

        String sql = "Call add_employee_details(?, ?, ?, ?, ?, ?, ?::bigint, ?, ?::bigint, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (CallableStatement stmt = connection.prepareCall(sql)) {

            stmt.setString(1, empDetails.getLastName());
            stmt.setString(2, empDetails.getFirstName());

            if (empDetails.getEmpBirthday() != null) {
                stmt.setDate(3, new java.sql.Date(empDetails.getEmpBirthday().getTime()));
            } else {
                stmt.setNull(3, java.sql.Types.DATE);
            }

            stmt.setString(4, empDetails.getEmpAddress());
            stmt.setString(5, empDetails.getEmpPhoneNumber());
            stmt.setString(6, empDetails.getEmpSSS());
            stmt.setLong(7, empDetails.getEmpPhilHealth());
            stmt.setString(8, empDetails.getEmpTIN());
            stmt.setLong(9, empDetails.getEmpPagibig());

            if (empDetails.getEmpStatus() != null) {
                stmt.setInt(10, empDetails.getEmpStatus().getId());
            } else {
                stmt.setNull(10, java.sql.Types.INTEGER);
            }
            if (empDetails.getEmpPosition() != null) {
                stmt.setInt(11, empDetails.getEmpPosition().getId());
            } else {
                stmt.setNull(11, java.sql.Types.INTEGER);
            }
            if (empDetails.getEmpImmediateSupervisor() != null) {
                stmt.setInt(12, empDetails.getEmpImmediateSupervisor().getEmpID());
            } else {
                stmt.setNull(12, java.sql.Types.INTEGER);
            }

            stmt.setBigDecimal(13, BigDecimal.valueOf(empDetails.getEmpBasicSalary()));
            stmt.setBigDecimal(14, BigDecimal.valueOf(empDetails.getEmpRice()));
            stmt.setBigDecimal(15, BigDecimal.valueOf(empDetails.getEmpPhone()));
            stmt.setBigDecimal(16, BigDecimal.valueOf(empDetails.getEmpClothing()));
            stmt.setBigDecimal(17, BigDecimal.valueOf(empDetails.getEmpMonthlyRate()));
            stmt.setBigDecimal(18, BigDecimal.valueOf(empDetails.getEmpHourlyRate()));

            stmt.registerOutParameter(19, java.sql.Types.INTEGER);

            stmt.execute();

            int generatedId = stmt.getInt(19);
            empDetails.setEmpID(generatedId);
            System.out.println("New employee created with ID: " + generatedId);
        } catch (SQLException e) {
            throw new DataAccessException("addEmployeeDetails failed", e);
        }

        return empDetails;
    }

    public boolean deleteEmployeeDetails(int empID) {
        boolean isDeleted = false;

        if (connection != null) {
            String sql = "Call delete_employee_details(?, ?)";

            try (CallableStatement stmt = connection.prepareCall(sql)) {
                stmt.setInt(1, empID);
                stmt.registerOutParameter(2, java.sql.Types.INTEGER);

                stmt.execute();

                int deletedCount = stmt.getInt(2);
                System.out.println("Deleted count from procedure: " + deletedCount);
                isDeleted = deletedCount > 0;
            } catch (SQLException e) {
                throw new DataAccessException("deleteEmployeeDetails failed", e);
            }
        }

        return isDeleted;
    }

    public List<Person> getAllEmployee(){
        List<Person> allEmployee = new ArrayList<>();
            if (connection != null) {
            String Query = "SELECT * FROM employee order by employee_id ASC";
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(Query);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()){
                    Person employeeDetails = toEmployeeDetails(resultSet);
                    allEmployee.add(employeeDetails);
                }
                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
            }
        }
        return allEmployee;
    }

    public List<Person> getEmployeeDirectory() {
        String sql = """
            SELECT e.*, p.position AS position_name, s.status AS status_name,
                   sup.firstname AS sup_firstname, sup.lastname AS sup_lastname
              FROM public.employee e
              LEFT JOIN public.employee_position p ON p.id = e.position
              LEFT JOIN public.employee_status s ON s.id = e.status
              LEFT JOIN public.employee sup ON sup.employee_id = e.immediate_supervisor
             ORDER BY e.employee_id
        """;
        List<Person> people = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Person person = new Employee();
                person.setEmpID(rs.getInt("employee_id"));
                person.setFirstName(rs.getString("firstname"));
                person.setLastName(rs.getString("lastname"));
                person.setEmpBirthday(rs.getDate("birthday"));
                person.setEmpAddress(rs.getString("address"));
                person.setEmpPhoneNumber(rs.getString("phone_number"));
                person.setEmpSSS(rs.getString("sss"));
                person.setEmpPhilHealth(rs.getLong("philhealth"));
                person.setEmpTIN(rs.getString("tin"));
                person.setEmpPagibig(rs.getLong("pag_ibig"));
                person.setEmpBasicSalary(rs.getDouble("basic_salary"));
                person.setEmpRice(rs.getDouble("rice_subsidy"));
                person.setEmpPhone(rs.getDouble("phone_allowance"));
                person.setEmpClothing(rs.getDouble("clothing_allowance"));
                person.setEmpMonthlyRate(rs.getDouble("gross_semi_monthly_rate"));
                person.setEmpHourlyRate(rs.getDouble("hourly_rate"));
                int positionId = rs.getInt("position");
                if (positionId > 0) {
                    EmployeePosition position = new EmployeePosition();
                    position.setId(positionId);
                    position.setPosition(rs.getString("position_name"));
                    person.setEmpPosition(position);
                }
                int statusId = rs.getInt("status");
                if (statusId > 0) {
                    EmployeeStatus status = new EmployeeStatus();
                    status.setId(statusId);
                    status.setStatus(rs.getString("status_name"));
                    person.setEmpStatus(status);
                }
                int supervisorId = rs.getInt("immediate_supervisor");
                if (supervisorId > 0) {
                    Person supervisor = new Employee();
                    supervisor.setEmpID(supervisorId);
                    supervisor.setFirstName(rs.getString("sup_firstname"));
                    supervisor.setLastName(rs.getString("sup_lastname"));
                    person.setEmpImmediateSupervisor(supervisor);
                }
                people.add(person);
            }
        } catch (SQLException e) {
            throw new DataAccessException("getEmployeeDirectory failed", e);
        }
        return people;
    }

    public int countSubordinates(int empID) {
        String sql = "SELECT count(*) FROM public.employee WHERE immediate_supervisor = ? AND employee_id <> ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ps.setInt(2, empID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("countSubordinates failed", e);
        }
    }

    public List<EmployeePosition> getAllPosition(){
        List<EmployeePosition> positions = new ArrayList<>();
        if (connection != null) {
        String Query = "SELECT * FROM employee_position";
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(Query);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()){
                    EmployeePosition position = new EmployeePosition();
                    position.setId(resultSet.getInt("id"));
                    position.setPosition(resultSet.getString("position"));
                    positions.add(position);
                }
                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
            }
        }
        return positions;
    }

     public List<EmployeeStatus> getAllStatuses(){
        List<EmployeeStatus> statuses = new ArrayList<>();
        if (connection != null) {
        String Query = "SELECT * FROM employee_status";
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(Query);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()){
                    EmployeeStatus status = new EmployeeStatus();
                    status.setId(resultSet.getInt("id"));
                    status.setStatus(resultSet.getString("status"));
                    statuses.add(status);
                }
                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
            }
        }
        return statuses;
    }

    public List<Employee> getEmpHoursByEmpID(int empID) {
        List<Employee> empHours = new ArrayList<>();

        if (connection == null) {
            System.err.println("Database connection is null. Cannot fetch employee hours.");
            return empHours;
        }

        String query = "SELECT * FROM public.vw_employee_hours WHERE employee_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, empID);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Employee e = new Employee();
                    e.setEmpID(resultSet.getInt("employee_id"));
                    e.setDate(resultSet.getDate("date"));
                    e.setTimeIn(resultSet.getObject("time_in", LocalTime.class));
                    e.setTimeOut(resultSet.getObject("time_out", LocalTime.class));
                    empHours.add(e);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error", e);
        }

        return empHours;
    }

    private Person toEmployeeDetails(ResultSet resultSet, boolean fetchSupervisor)
        throws SQLException {
        Person employeeDetails = new Employee();
            employeeDetails.setEmpID(resultSet.getInt("employee_id"));
            employeeDetails.setFirstName(resultSet.getString("firstname"));
            employeeDetails.setLastName(resultSet.getString("lastname"));
            employeeDetails.setEmpBirthday(resultSet.getDate("birthday"));
            employeeDetails.setEmpAddress(resultSet.getString("address"));
            employeeDetails.setEmpPhoneNumber(resultSet.getString("phone_number"));
            employeeDetails.setEmpSSS(resultSet.getString("sss"));
            employeeDetails.setEmpPhilHealth(resultSet.getLong("philhealth"));
            employeeDetails.setEmpTIN(resultSet.getString("tin"));
            employeeDetails.setEmpPagibig(resultSet.getLong("pag_ibig"));
            employeeDetails.setEmpBasicSalary(resultSet.getDouble("basic_salary"));
            employeeDetails.setEmpRice(resultSet.getDouble("rice_subsidy"));
            employeeDetails.setEmpPhone(resultSet.getDouble("phone_allowance"));
            employeeDetails.setEmpClothing(resultSet.getDouble("clothing_allowance"));
            employeeDetails.setEmpMonthlyRate(resultSet.getDouble("gross_semi_monthly_rate"));
            employeeDetails.setEmpHourlyRate(resultSet.getDouble("hourly_rate"));
            int superVisorId  = resultSet.getInt("immediate_supervisor");
                if (superVisorId > 0 && fetchSupervisor){
                    Person superVisor = getByEmpID(superVisorId, false);
                    employeeDetails.setEmpImmediateSupervisor(superVisor);
                }
            int positionId  = resultSet.getInt("position");
                if (positionId > 0){
                    EmployeePosition position = getPositionById(positionId);
                    employeeDetails.setEmpPosition(position);
                }
           int statusId  = resultSet.getInt("status");
                if (statusId > 0){
                    EmployeeStatus status = getStatusById(statusId);
                    employeeDetails.setEmpStatus(status);
                }
        return employeeDetails;
    }

    private Person toEmployeeDetails(ResultSet resultSet)
        throws SQLException {
        return toEmployeeDetails(resultSet, true);
    }
}
