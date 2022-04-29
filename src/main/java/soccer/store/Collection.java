package soccer.store;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;

public abstract class Collection<T_Record> {

    private final String       _name;
    private final String[]     _fieldArray;
    private final StoreService _storeService;

    public Collection(String name, String[] fieldArray, StoreService storeService) {

        _name = name;
        _fieldArray = fieldArray;
        _storeService = storeService;
    }

    protected abstract String[] primaryFields();

    protected abstract Map<String, String> updateFields(T_Record record);

    protected abstract T_Record to(ResultSet resultSet) throws SQLException;

    protected abstract String[] from(T_Record record);

    protected String name() {

        return _name;
    }

    protected String[] fieldArray() {

        return _fieldArray;
    }

    protected StoreService storeService() {

        return _storeService;
    }

    protected String join(String... strings) {

        return String.join("", strings);
    }

    protected String joinComma(String... strings) {

        return String.join(",", strings);
    }

    protected String textEntry(String entryId) {

        return entryId + " text NOT NULL";
    }

    protected String primaryKeyEntry(String... entryArray) {

        return "PRIMARY KEY (" + joinComma(entryArray) + ")";
    }

    protected Connection connection() throws SQLException {

        return DriverManager.getConnection(storeService().databaseUrl(),
                                           storeService().storeUser(),
                                           storeService().storePassword());
    }

    private RestOutput<Boolean> probeRelation(String relationId) {

        if (Api.isNull(relationId)) {
            return RestOutput.badRequest();
        }

        try (Connection connection = connection()) {
            try (Statement statement = connection.createStatement()) {

                ResultSet resultSet = statement.executeQuery("SELECT to_regclass ('" + relationId + "');");

                if (resultSet.next() == false) {
                    return RestOutput.FALSE;
                }
                // Check if this is the NULL entry
                resultSet.getString(1);
                if (resultSet.wasNull()) {
                    return RestOutput.FALSE;
                }

                return RestOutput.TRUE;
            }
        } catch (Throwable t) {
            Api.error(t, "Failure to probeRelation. INTERNAL FAILURE", relationId, this);
            return RestOutput.internalFailure();
        }
    }

    private RestOutput<Result> createTable(String... entryArray) {

        String updateSQL;

        if (Api.isNullArray(entryArray)) {
            return RestOutput.badRequest();
        }

        updateSQL = join("CREATE TABLE ", name(), " (", joinComma(entryArray), ");");

        try (Connection connection = connection()) {
            try (Statement statement = connection.createStatement()) {

                statement.executeUpdate(updateSQL);

                return RestOutput.OK;
            }
        } catch (Throwable t) {
            Api.error(t, "Failure to createTable. INTERNAL FAILURE", updateSQL, this);
            return RestOutput.internalFailure();
        }
    }

    protected RestOutput<Result> init(String... entryArray) {

        RestOutput<Boolean> probeOutput;

        if (Api.isNullArray(entryArray)) {
            return RestOutput.badRequest();
        }

        probeOutput = probeRelation(name());
        if (RestOutput.isNOK(probeOutput)) {
            Api.error("probeRelation to init is NOT OK", probeOutput, this);
            return RestOutput.of(probeOutput);
        }
        if (probeOutput.output()) {
            return RestOutput.OK;
        }

        // Create the table as it does not exist yet
        return createTable(entryArray);
    }

    protected RestOutput<Result> storeRecord(T_Record record) {

        String[] valueArray;
        String updateSQL;
        Map<String, String> updateRecordMap;

        if (Api.isNull(record)) {
            return RestOutput.badRequest();
        }

        valueArray = from(record);
        if (valueArray == null) {
            Api.error("storeRecord failed. INTERNAL FAILURE", record, this);
            return RestOutput.internalFailure();
        }

        // Add single quote for each text value
        valueArray = Stream.of(valueArray).map(value -> "'" + value + "'").toArray(String[]::new);

        updateRecordMap = updateFields(record);
        if (updateRecordMap.isEmpty()) {

            // INSERT only of the record
            updateSQL = join("INSERT INTO ",
                             name(),
                             "(",
                             joinComma(fieldArray()),
                             ") VALUES (",
                             joinComma(valueArray),
                             ");");

        } else {

            // INSERT or UPDATE of the record
            updateSQL = join("INSERT INTO ",
                             name(),
                             "(",
                             joinComma(fieldArray()),
                             ") VALUES (",
                             joinComma(valueArray),
                             ") ON CONFLICT (",
                             joinComma(primaryFields()),
                             ") DO UPDATE SET ",
                             joinComma(updateRecordMap.entrySet()
                                                      .stream()
                                                      .map(entry -> entry.getKey() + "='" + entry.getValue() + "'")
                                                      .toArray(String[]::new)),
                             ";");
        }

        try (Connection connection = connection()) {
            try (Statement statement = connection.createStatement()) {

                statement.executeUpdate(updateSQL);

                return RestOutput.OK;
            }
        } catch (Throwable t) {
            Api.error(t, "Failure to storeRecord. INTERNAL FAILURE", updateSQL, this);
            return RestOutput.internalFailure();
        }
    }

    protected RestOutput<List<T_Record>> loadRecords() {

        List<T_Record> recordList;
        T_Record record;
        String querySQL;

        recordList = new ArrayList<T_Record>();

        querySQL = join("SELECT * FROM ", name(), ";");

        // Execute the Query
        try (Connection connection = connection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(querySQL)) {

                    while (resultSet.next()) {

                        // Generate a Record from the resultSet
                        record = to(resultSet);
                        if (record == null) {
                            Api.error("loadRecords failed. INTERNAL FAILURE", record, this);
                            return RestOutput.internalFailure();
                        }
                        recordList.add(record);
                    }

                    return RestOutput.ok(recordList);
                }
            }
        } catch (Throwable t) {
            Api.error(t, "Failure to loadRecords. INTERNAL FAILURE", querySQL, this);
            return RestOutput.internalFailure();
        }
    }

    @Override
    public String toString() {
        return "Collection [_name=" + _name + ", _fieldArray=" + _fieldArray + "]";
    }

}
