package ch.so.agi.gretl.jobs;

import ch.so.agi.gretl.util.GradleVariable;
import ch.so.agi.gretl.util.IntegrationTestUtil;
import ch.so.agi.gretl.util.IntegrationTestUtilSql;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;



public class Db2DbTaskTest {
    static String WAIT_PATTERN = ".*database system is ready to accept connections.*\\s";
    
    @ClassRule
    public static PostgreSQLContainer postgres = 
        (PostgreSQLContainer) new PostgisContainerProvider()
        .newInstance().withDatabaseName("gretl")
        .withUsername(IntegrationTestUtilSql.PG_CON_DDLUSER)
        .withInitScript("init_postgresql.sql")
        .waitingFor(Wait.forLogMessage(WAIT_PATTERN, 2));
    
	/*
	 * Tests if fetchSize parameter is working.
	 * Gradle throws an error if a parameter is being
	 * used that is not defined in the task class.
	 */
	@Test
	public void fetchSizeParameterTest() throws Exception {
		String schemaName = "db2dbTaskFetchSize".toLowerCase();
		Connection con = null;
		try {
			con = IntegrationTestUtilSql.connectPG(postgres);
            IntegrationTestUtilSql.createOrReplaceSchema(con, schemaName);

            Statement stmt = con.createStatement();
            stmt.execute("CREATE TABLE "+schemaName+".source_data(t_id serial, aint integer, adec decimal(7,1), atext varchar(40), aenum varchar(120),adate date, atimestamp timestamp, aboolean boolean,geom_so geometry(POINT,2056))");
            stmt.execute("INSERT INTO "+schemaName+".source_data(t_id, aint, adec, atext, adate, atimestamp, aboolean, geom_so) VALUES (1,2,3.4,'abc','2013-10-21','2015-02-16T08:35:45.000','true',ST_GeomFromText('POINT(2638000.0 1175250.0)',2056))");
            stmt.execute("INSERT INTO "+schemaName+".source_data(t_id, aint, adec, atext, adate, atimestamp, aboolean, geom_so) VALUES (2,33,44.4,'asdf','2017-12-21','2015-03-16T11:35:45.000','true',ST_GeomFromText('POINT(2648000.0 1185250.0)',2056))");
            
            stmt.execute("CREATE TABLE "+schemaName+".target_data(t_id serial, aint integer, adec decimal(7,1), atext varchar(40), aenum varchar(120),adate date, atimestamp timestamp, aboolean boolean,geom_so geometry(POINT,2056))");

            stmt.close();
            IntegrationTestUtilSql.grantDataModsInSchemaToUser(con, schemaName, IntegrationTestUtilSql.PG_CON_DMLUSER);

            con.commit();
            IntegrationTestUtilSql.closeCon(con);

		    GradleVariable[] gvs = {GradleVariable.newGradleProperty(IntegrationTestUtilSql.VARNAME_PG_CON_URI, postgres.getJdbcUrl())};
		    IntegrationTestUtil.runJob("src/integrationTest/jobs/Db2DbTaskFetchSize", gvs);
            
            con = IntegrationTestUtilSql.connectPG(postgres);
            String countDestSql = String.format("select count(*) from %s.target_data", schemaName);
            int countDest = IntegrationTestUtilSql.execCountQuery(con, countDestSql);

            Assert.assertEquals(
                    "Rowcount in table source_data must be equal to rowcount in table target_data",
                    2,
                    countDest);
		}
		finally {
            IntegrationTestUtilSql.closeCon(con);
        }
	}
	
	
	
	
    /*
Test's that a chain of statements executes properly and results in the correct
number of inserts (corresponding to the last statement)
    1. Statement transfers rows from a to b
    2. Statement transfers rows from b to a
*/
    @Test
    public void taskChainTest() throws Exception {
        String schemaName = "db2dbTaskChain".toLowerCase();
        Connection con = null;
        try{
            con = IntegrationTestUtilSql.connectPG(postgres);
            IntegrationTestUtilSql.createOrReplaceSchema(con, schemaName);
            int countSrc = IntegrationTestUtilSql.prepareDb2DbChainTables(con, schemaName);
            con.commit();
            IntegrationTestUtilSql.closeCon(con);

            GradleVariable[] gvs = {GradleVariable.newGradleProperty(IntegrationTestUtilSql.VARNAME_PG_CON_URI, postgres.getJdbcUrl())};
            IntegrationTestUtil.runJob("src/integrationTest/jobs/Db2DbTaskChain", gvs);

            //reconnect to check results
            con = IntegrationTestUtilSql.connectPG(postgres);
            String countDestSql = String.format("select count(*) from %s.albums_dest", schemaName);
            int countDest = IntegrationTestUtilSql.execCountQuery(con, countDestSql);

            Assert.assertEquals(
                    "Rowcount in table albums_src must be equal to rowcount in table albums_dest",
                    countSrc,
                    countDest);
        }
        finally {
            IntegrationTestUtilSql.closeCon(con);
        }
    }

    /**
     * Test's if the sql-files can be configured using a relative path.
     *
     * The relative path relates to the location of the build.gradle file
     * of the corresponding gretl job.
     */
    @Test
    public void relativePathTest() throws Exception{
        String schemaName = "relativePath".toLowerCase();
        Connection con = null;
        try{
            con = IntegrationTestUtilSql.connectPG(postgres);
            IntegrationTestUtilSql.createOrReplaceSchema(con, schemaName);

            int countSrc = IntegrationTestUtilSql.prepareDb2DbChainTables(con, schemaName);
            con.commit();
            IntegrationTestUtilSql.closeCon(con);

            GradleVariable[] gvs = {GradleVariable.newGradleProperty(IntegrationTestUtilSql.VARNAME_PG_CON_URI, postgres.getJdbcUrl())};
            IntegrationTestUtil.runJob("src/integrationTest/jobs/Db2DbTaskRelPath", gvs);

            //reconnect to check results
            con = IntegrationTestUtilSql.connectPG(postgres);
            String countDestSql = String.format("select count(*) from %s.albums_dest", schemaName);
            int countDest = IntegrationTestUtilSql.execCountQuery(con, countDestSql);

            Assert.assertEquals(
                    "Rowcount in table albums_src must be equal to rowcount in table albums_dest",
                    countSrc,
                    countDest);
        }
        finally {
            IntegrationTestUtilSql.closeCon(con);
        }
    }

    /**
     * Test's that the delete flag of the Db2dbTask's Transferset works properly
     */
    @Test
    public void deleteDestTableContent() throws Exception{
        String schemaName = "deleteDestTableContent".toLowerCase();
        Connection con = null;
        try{
            con = IntegrationTestUtilSql.connectPG(postgres);
            IntegrationTestUtilSql.createOrReplaceSchema(con, schemaName);

            int countSrc = IntegrationTestUtilSql.prepareDb2DbChainTables(con, schemaName);
            IntegrationTestUtilSql.insertRowsInAlbumsTable(con, schemaName, "dest", 3);

            con.commit();
            IntegrationTestUtilSql.closeCon(con);

            GradleVariable[] gvs = {GradleVariable.newGradleProperty(IntegrationTestUtilSql.VARNAME_PG_CON_URI, postgres.getJdbcUrl())};
            IntegrationTestUtil.runJob("src/integrationTest/jobs/Db2DbTaskDelTable", gvs);

            //reconnect to check results
            con = IntegrationTestUtilSql.connectPG(postgres);
            String countDestSql = String.format("select count(*) from %s.albums_dest", schemaName);
            int countDest = IntegrationTestUtilSql.execCountQuery(con, countDestSql);

            Assert.assertEquals(
                    "Rowcount in table albums_src must be equal to rowcount in table albums_dest",
                    countSrc,
                    countDest);
        }
        finally {
            IntegrationTestUtilSql.closeCon(con);
        }
    }
}
