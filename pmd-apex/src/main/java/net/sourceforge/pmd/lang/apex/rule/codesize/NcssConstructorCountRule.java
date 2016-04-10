/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.lang.apex.rule.codesize;

import net.sourceforge.pmd.lang.apex.ast.ASTMethod;
import net.sourceforge.pmd.lang.apex.ast.ASTMethodCallExpression;
import net.sourceforge.pmd.stat.DataPoint;
import net.sourceforge.pmd.util.NumericConstants;

/**
 * Non-commented source statement counter for constructors.
 * 
 * @author ported from Java original by Jason Bennett
 */
public class NcssConstructorCountRule extends AbstractNcssCountRule {

	/**
	 * Count constructor declarations. This includes any explicit super() calls.
	 */
	public NcssConstructorCountRule() {
		super(ASTMethod.class);
		setProperty(MINIMUM_DESCRIPTOR, 100d);
	}

	public Object visit(ASTMethodCallExpression node, Object data) {
		return NumericConstants.ONE;
	}

	@Override
	public Object[] getViolationParameters(DataPoint point) {
		// TODO need to put class name or constructor ID in string
		return new String[] { String.valueOf(((ASTMethod) point.getNode()).getNode().getMethodInfo().getName()),
				String.valueOf((int) point.getScore()) };
	}
}
