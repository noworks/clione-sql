package tetz42.clione.lang;

import static org.junit.Assert.*;
import static tetz42.clione.SQLManager.*;
import static tetz42.test.Auty.*;

import java.util.Map;

import org.junit.Test;

import tetz42.clione.common.HereDoc;
import tetz42.clione.exception.ClioneFormatException;

public class UpperIfTest {

	private static final Map<String, String> doc = HereDoc
			.get(UpperIfTest.class);

	@Test
	public void IF_block1() {
		String sql = sqlManager().useSQL(doc.get("test")).generateSql(
				paramsOn("block1", "block2", "block3").$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block1");
	}

	@Test
	public void IF_block2() {
		String sql = sqlManager().useSQL(doc.get("test")).generateSql(
				paramsOn("block2", "block3").$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block2");
	}

	@Test
	public void IF_block3() {
		String sql = sqlManager().useSQL(doc.get("test")).generateSql(
				paramsOn("block3").$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block3");
	}

	@Test
	public void IF_block4_ELSE() {
		String sql = sqlManager().useSQL(doc.get("test")).generateSql(
				params().$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block4_ELSE");
	}

	@Test
	public void IF_block1_no_child() {
		String sql = sqlManager().useSQL(doc.get("test")).generateSql(
				paramsOn("block1", "block2", "block3").$("tako", 800).$("ika",
						100));
		assertEqualsWithFile(sql, getClass(), "IF_block1_no_child");
	}

	@Test
	public void IF_block1_child1() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("block1", "block2", "block3", "childBlock1",
						"childBlock2").$("tako", 800).$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block1_child1");
	}

	@Test
	public void IF_block1_child2() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("block1", "block2", "block3", "childBlock2").$("tako",
						800).$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block1_child2");
	}

	@Test
	public void IF_block2_no_child() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("block2", "block3").$("tako", 800).$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block2_no_child");
	}

	@Test
	public void IF_block2_child1() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("block2", "block3", "childBlock1").$("tako", 800).$(
						"ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block2_child1");
	}

	@Test
	public void IF_block2_child2() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("block2", "block3", "childBlock2").$("tako", 800).$(
						"ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block2_child2");
	}

	@Test
	public void IF_block3_child() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("block3", "childBlock").$("tako", 800).$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block3_child");
	}

	@Test
	public void IF_block3_childElse() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("block3").$("tako", 800).$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block3_childElse");
	}

	@Test
	public void IF_block4_ELSE_no_child() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				params().$("tako", 800).$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block4_ELSE_no_child");
	}

	@Test
	public void IF_block4_ELSE_child1() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("childBlock1").$("tako", 800).$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block4_ELSE_child1");
	}

	@Test
	public void IF_block4_ELSE_child2() {
		String sql = sqlManager().useSQL(doc.get("nest")).generateSql(
				paramsOn("childBlock2").$("tako", 800).$("ika", 100));
		assertEqualsWithFile(sql, getClass(), "IF_block4_ELSE_child2");
	}

	@Test
	public void IFP_block1() {
		String sql = sqlManager().useSQL(doc.get("parenthesis")).generateSql(
				paramsOn("block1").$("tako", 800).$("ika", 1000));
		assertEqualsWithFile(sql, getClass(), "IFP_block1");
	}

	@Test
	public void IFP_block2() {
		String sql = sqlManager().useSQL(doc.get("parenthesis")).generateSql(
				paramsOn("block2").$("tako", 800).$("ika", 1000));
		assertEqualsWithFile(sql, getClass(), "IFP_block2");
	}

	@Test
	public void IFP_ELSE() {
		String sql = sqlManager().useSQL(doc.get("parenthesis")).generateSql(
				params("tako", 800).$("ika", 1000));
		assertEqualsWithFile(sql, getClass(), "IFP_ELSE");
	}

	@Test
	public void withEnd_C() {
		String sql = sqlManager().useSQL(doc.get("withEnd")).generateSql(
				paramsOn("C"));
		assertEqualsWithFile(sql, getClass(), "withEnd_C");
	}

	@Test
	public void withEnd_D() {
		String sql = sqlManager().useSQL(doc.get("withEnd")).generateSql(
				paramsOn("D"));
		assertEqualsWithFile(sql, getClass(), "withEnd_D");
	}

	@Test
	public void withEnd_null() {
		String sql = sqlManager().useSQL(doc.get("withEnd")).generateSql();
		assertEqualsWithFile(sql, getClass(), "withEnd_null");
	}

	@Test(expected = ClioneFormatException.class)
	public void IF_nothing() {
		try {
			sqlManager().useSQL(doc.get("nothing")).generateSql(
					params("tako", 800).$("ika", 1000));
			fail();
		} catch (ClioneFormatException e) {
			assertEqualsWithFile(e.getMessage(), getClass(), "IF_nothing");
			throw e;
		}
	}
}
