/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Gideon Software
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, visit https://www.gideonsoftware.com
 */

package com.gideonsoftware.mist.tntapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;

import javax.money.MonetaryAmount;

import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.FastMoney;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.TntDbException;

@RunWith(JUnit4.class)
public class TntDbTest {

    private static Logger log;

    private static String dbName;
    private static String dbPath;

    private static Integer BAMBIDEER_CONTACTID;
    private static Integer BAMBIDEER_HISTORYID;
    private static String BAMBIDEER_HISTORY_DATE_STR;
    private static String BAMBIDEER_HISTORY_SUBJECT;

    public static void createTestData() {

        //
        // Bambi Deer
        // An existing history item
        //

        BAMBIDEER_CONTACTID = Integer.valueOf(272072119);
        BAMBIDEER_HISTORYID = Integer.valueOf(537624034); // An email
        BAMBIDEER_HISTORY_DATE_STR = "#2007-11-06 15:27:21#";
        BAMBIDEER_HISTORY_SUBJECT = "Email tasks this color (BLACK)";
    }

    @BeforeClass
    public static void setup() {
        MIST.configureLogging(TntDbTest.class);
        setupTntDb();
        createTestData();
    }

    public static void setupTntDb() {
        dbName = "Toontown.mpddb";

        try {
            Path source = Paths.get(new TntDbTest().getClass().getResource(dbName).toURI());
            Path dest = Paths.get(System.getProperty("java.io.tmpdir") + "/" + dbName);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            dbPath = dest.toString();
        } catch (URISyntaxException | IOException e) {
            log.error(e);
            fail("Could not set up the tests properly!");
        }
        String[] opts = { "--profile=tntdbtest" };
        MIST.parseOptions(opts);
        TntDb.setTntDatabasePath(dbPath);
        TntDb.setUseCommit(false);

        try {
            TntDb.connect(true);
        } catch (TntDbException e) {
            fail("Unable to connect to TntConnect database!");
        }
    }

    @AfterClass
    public static void teardown() {
        teardownTntDb();
    }

    public static void teardownTntDb() {
        try {
            TntDb.disconnect();
            Files.deleteIfExists(Paths.get(dbPath));
        } catch (IOException e) {
            log.error(e);
            fail("Database shouldn't have trouble being deleted!");
        }
    }

    /**
     * Tests creating insertion query from 2D String array
     */
    @Test
    public void createInsertQuery() {
        String tableName = "MyTableName";
        String[][] colValuePairs = { { "Col1", "'Value1'" }, { "Col2", "'Value2'" }, { "Col3", "'Value3'" } };
        String expected = "INSERT INTO [MyTableName] ([Col1],[Col2],[Col3]) VALUES ('Value1','Value2','Value3')";
        assertEquals(expected, TntDb.createInsertQuery(tableName, colValuePairs));
    }

    /**
     * Tests converting float to money
     */
    @Test
    public void floatToMoney() {
        float[] input = { 0, 10, 100000, (float) 10.0, (float) 10.1, (float) 10.11, (float) 10.111, (float) 10.115 };
        FastMoney[] expected = {
            FastMoney.of(0, "USD"),
            FastMoney.of(10, "USD"),
            FastMoney.of(100000, "USD"),
            FastMoney.of(10, "USD"),
            FastMoney.of(10.1, "USD"),
            FastMoney.of(10.11, "USD"),
            FastMoney.of(10.11, "USD"),
            FastMoney.of(10.12, "USD") };
        for (int i = 0; i < expected.length; i++)
            assertEquals(String.valueOf(expected[i]), TntDb.floatToMoney(input[i]).toString());
    }

    /**
     * Tests formatting DB booleans
     */
    @Test
    public void formatDbBoolean() {
        assertEquals("-1", TntDb.formatDbBoolean(true));
        assertEquals("0", TntDb.formatDbBoolean(false));
    }

    /**
     * Tests formatting DB currency
     */
    @Test
    public void formatDbCurrency() {
        MonetaryAmount[] input = {
            null,
            FastMoney.of(0, "USD"),
            FastMoney.of(100, "USD"),
            FastMoney.of(200.0, "USD"),
            FastMoney.of(300.10, "USD"),
            FastMoney.of(400.100, "USD"),
            FastMoney.of(500.101, "USD") };
        String[] expected = { "NULL", "0.00000", "100.00000", "200.00000", "300.10000", "400.10000", "500.10100" };
        for (int i = 0; i < expected.length; i++)
            assertEquals(expected[i], TntDb.formatDbCurrency(input[i]));
    }

    /**
     * Tests formatting DB dates with and without time
     */
    @Test
    public void formatDbDate() throws ParseException {
        assertEquals("NULL", TntDb.formatDbDate(null));
        assertEquals("NULL", TntDb.formatDbDateNoTime(null));
        LocalDateTime date = LocalDateTime.of(2017, 9, 30, 13, 45, 24);
        assertEquals("#2017-09-30 13:45:24#", TntDb.formatDbDate(date));
        assertEquals("#2017-09-30#", TntDb.formatDbDateNoTime(date));
        date = LocalDateTime.of(2007, 11, 6, 15, 27, 21);
        assertEquals("#2007-11-06 15:27:21#", TntDb.formatDbDate(date));
        assertEquals("#2007-11-06#", TntDb.formatDbDateNoTime(date));
    }

    /**
     * Tests formatting DB ints
     */
    @Test
    public void formatDbInt() {
        Integer[] input = { null, 0, 10, 100000 };
        String[] expected = { "NULL", "0", "10", "100000" };
        for (int i = 0; i < expected.length; i++)
            assertEquals(expected[i], TntDb.formatDbInt(input[i]));
    }

    /**
     * Tests formatting DB strings
     */
    @Test
    public void formatDbString() {
        String[][] tests = {
            { "", "''" },
            { null, "NULL" },
            { "test", "'test'" },
            { "'test'", "'''test'''" },
            { "What''s up?", "'What''''s up?'" } };
        for (int i = 0; i < tests.length; i++)
            assertEquals(tests[i][1], TntDb.formatDbString(tests[i][0]));

        String[][] testsMaxLen = {
            { "", "''" },
            { null, "NULL" },
            { "test", "'test'" },
            { "'test'", "'''test'" },
            { "What''s up?", "'What'''" } };
        for (int i = 0; i < testsMaxLen.length; i++)
            assertEquals(testsMaxLen[i][1], TntDb.formatDbString(testsMaxLen[i][0], 6));
    }

    /**
     * Tests getting available ids from History and HistoryContact tables
     */
    @Test
    public void getAvailableId() throws SQLException {
        // Try this multiple times
        // Doesn't guarantee that it works, but gives some assurance!
        for (int i = 0; i < 100; i++) {
            TntDb.getAvailableId(TntDb.TABLE_HISTORYCONTACT);
            TntDb.getAvailableId(TntDb.TABLE_HISTORY);
        }

        // Now with only positives
        for (int i = 0; i < 100; i++) {
            assertTrue(TntDb.getAvailableId(TntDb.TABLE_HISTORYCONTACT, true) >= 0);
            assertTrue(TntDb.getAvailableId(TntDb.TABLE_HISTORY, true) >= 0);
        }
    }

    /**
     * Tests getting the Description fields of various tables
     */
    @Test
    public void getDescription() throws SQLException, TntDbException {
        // Descriptions
        assertEquals("Call for Appt", TntDb.getDescription(TntDb.TABLE_MPDPHASE, 30));
        assertEquals("Ask in Future", TntDb.getMpdPhaseDescription(20));

        assertEquals("Newsletter", TntDb.getDescription(TntDb.TABLE_TASKTYPE, 60));
        assertEquals("Email", TntDb.getTaskTypeDescription(100));

        assertEquals("annual", TntDb.getDescription(TntDb.TABLE_PLEDGEFREQUENCY, 12));
    }

    /**
     * Tests getting one when there are multiple values
     */
    @Test
    public void getOneX_MultipleValues() throws TntDbException, SQLException {
        assertEquals(null, ContactManager.getFileAs(0)); // Verify initial state
        try {
            TntDb.getOneX("SELECT [ContactID] FROM [Contact] WHERE [ContactID] > 0", TntDb.TYPE_INT);
            fail("Can't get 'one' when multiple values exist");
        } catch (TntDbException e) {
        }
    }

    /**
     * Tests getting one when there is no value
     */
    @Test
    public void getOneX_NoValues() throws TntDbException, SQLException {
        assertEquals(null, ContactManager.getFileAs(0)); // Verify initial state
        Object obj = TntDb.getOneX("SELECT [ContactID] FROM [Contact] WHERE [ContactID] = 0", TntDb.TYPE_INT);
        assertEquals(null, obj);
    }

    /**
     * Tests getting one of various types
     */
    @Test
    public void getOneX_OneValue() throws TntDbException, SQLException {
        Object obj = null;

        // Null values
        assertEquals(null, TntDb.getOneX(null, null));
        assertEquals(null, TntDb.getOneX(null, TntDb.TYPE_INT));
        assertEquals(null, TntDb.getOneX("SELECT * FROM [Contact] LIMIT 1", null));

        // Integer
        String intQueryStr = String.format(
            "SELECT [HistoryID] FROM [History] WHERE [HistoryID] = %s",
            BAMBIDEER_HISTORYID);
        obj = TntDb.getOneX(intQueryStr, TntDb.TYPE_INT);
        assertEquals(true, obj instanceof Integer);
        assertEquals(BAMBIDEER_HISTORYID, obj);

        Integer intResult = TntDb.getOneInt(intQueryStr);
        assertEquals(BAMBIDEER_HISTORYID, intResult);
        assertEquals(null, TntDb.getOneInt(null));

        // Date
        String dateQueryStr = String.format(
            "SELECT [HistoryDate] FROM [History] WHERE [HistoryID] = %s",
            BAMBIDEER_HISTORYID);
        obj = TntDb.getOneX(dateQueryStr, TntDb.TYPE_DATE);
        assertEquals(true, obj instanceof LocalDateTime);
        assertEquals(BAMBIDEER_HISTORY_DATE_STR, TntDb.formatDbDate((LocalDateTime) obj));

        LocalDateTime dateResult = TntDb.getOneDate(dateQueryStr);
        assertEquals(BAMBIDEER_HISTORY_DATE_STR, TntDb.formatDbDate(dateResult));
        assertEquals(null, TntDb.getOneDate(null));

        // String
        String strQueryStr = String.format(
            "SELECT [Description] FROM [History] WHERE [HistoryID] = %s",
            BAMBIDEER_HISTORYID);
        obj = TntDb.getOneX(strQueryStr, TntDb.TYPE_STRING);
        assertEquals(true, obj instanceof String);
        assertEquals(BAMBIDEER_HISTORY_SUBJECT, obj);

        String strResult = TntDb.getOneString(strQueryStr);
        assertEquals(BAMBIDEER_HISTORY_SUBJECT, strResult);
        assertEquals(null, TntDb.getOneString(null));

        // Multiple columns is also allowed, but only first column's value is returned
        obj = TntDb.getOneX(
            String.format(
                "SELECT [Description], [LastEdit] FROM [History] WHERE [HistoryID] = %s",
                BAMBIDEER_HISTORYID),
            TntDb.TYPE_STRING);
        assertEquals(true, obj instanceof String);
        assertEquals(BAMBIDEER_HISTORY_SUBJECT, obj);
    }

    /**
     * Tests getRowCount using pre-defined Contact table
     */
    @Test
    public void getRowCount() throws SQLException, TntDbException {
        ResultSet rs = null;
        rs = TntDb.runQuery("SELECT * FROM [Contact] WHERE [FileAs] LIKE 'Nobody'");
        assertEquals(0, TntDb.getRowCount(rs));
        rs = TntDb.runQuery("SELECT * FROM [Contact] WHERE [FileAs] LIKE 'Kent%'");
        assertEquals(1, TntDb.getRowCount(rs)); // Clark & Lois
        rs = TntDb.runQuery("SELECT * FROM [Contact] WHERE [FileAs] LIKE 'Bear%'");
        assertEquals(2, TntDb.getRowCount(rs)); // Yogi, Baloo & Balinda
    }

    /**
     * Tests getting an Integer from a ResultSet
     */
    @Test
    public void getRSInteger() throws TntDbException, SQLException {
        ResultSet rs = TntDb.runQuery("SELECT * FROM [Contact] WHERE [ContactID] = " + BAMBIDEER_CONTACTID);
        rs.next();
        assertEquals(Integer.valueOf(272072119), TntDb.getRSInteger(rs, "ContactID"));
        assertEquals(Integer.valueOf(840), TntDb.getRSInteger(rs, "HomeCountryID"));
        assertEquals(null, TntDb.getRSInteger(rs, "AnniversaryYear"));
    }

    /**
     * Rolls back the database after each test
     */
    @After
    public void rollback() throws SQLException, TntDbException {
        TntDb.rollback();
        assertEquals(TntDb.getConnection().getAutoCommit(), false);
    }

    /**
     * Tests updating a table's LastEdit field
     */
    @Test
    public void updateTableLastEdit() throws SQLException, TntDbException {
        LocalDateTime prevDate = TntDb.getOneDate(
            String.format("SELECT [LastEdit] FROM [Contact] WHERE [ContactID] = %s", BAMBIDEER_CONTACTID));
        TntDb.updateTableLastEdit("Contact", BAMBIDEER_CONTACTID);
        LocalDateTime newDate = TntDb.getOneDate(
            String.format("SELECT [LastEdit] FROM [Contact] WHERE [ContactID] = %s", BAMBIDEER_CONTACTID));
        System.out.println(prevDate);
        System.out.println(newDate);
        assertTrue(newDate.compareTo(prevDate) > 0); // New date is newer than prev date
        assertTrue(newDate.plusMinutes(1).compareTo(LocalDateTime.now()) > 0); // A minute later is in the future
    }
}
