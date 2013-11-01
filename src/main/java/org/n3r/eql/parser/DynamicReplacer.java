package org.n3r.eql.parser;

import com.google.common.base.Objects;
import org.n3r.eql.ex.EqlExecuteException;
import org.n3r.eql.map.EqlDynamic;
import org.n3r.eql.map.EqlRun;
import org.n3r.eql.util.EqlUtils;

import java.util.List;

public class DynamicReplacer {
    private EqlDynamic eqlDynamic;
    private Object[] dynamics;
    private EqlRun eqlRun;

    public void replaceDynamics(EqlRun eqlRun, Object[] dynamics) {
        this.eqlRun = eqlRun;
        this.dynamics = dynamics;
        if (dynamics != null && dynamics.length > 0 && eqlRun.getEqlDynamic() == null)
            eqlRun.setEqlDynamic(new DynamicParser().parseRawSql(eqlRun.getRunSql()));

        eqlDynamic = eqlRun.getEqlDynamic();
        if (eqlDynamic == null) return;

        List<String> sqlPieces = eqlDynamic.getSqlPieces();
        StringBuilder runSql = new StringBuilder(sqlPieces.get(0));
        int ii = sqlPieces.size();

        switch (eqlDynamic.getPlaceholdertype()) {
            case AUTO_SEQ:
                for (int i = 1; i < ii; ++i)
                    runSql.append(findDynamicByIdx(i - 1)).append(sqlPieces.get(i));
                break;
            case MANU_SEQ:
                for (int i = 1; i < ii; ++i)
                    runSql.append(findDynamicBySeq(i - 1)).append(sqlPieces.get(i));
                break;
            case VAR_NAME:
                for (int i = 1; i < ii; ++i)
                    runSql.append(findDynamicByName(i - 1)).append(sqlPieces.get(i));
                break;
            default:
                break;
        }

        eqlRun.setRunSql(runSql.toString());
    }

    private Object findDynamicByIdx(int index) {
        if (index < dynamics.length) return dynamics[index];

        throw new EqlExecuteException("[" + eqlRun.getSqlId() + "] lack dynamic params");
    }

    private Object findDynamicBySeq(int index) {
        return findDynamicByIdx(eqlDynamic.getPlaceholders()[index].getSeq() - 1);
    }

    private Object findDynamicByName(int index) {
        Object bean = dynamics[0];

        String varName = eqlDynamic.getPlaceholders()[index].getPlaceholder();
        Object property = EqlUtils.getPropertyQuietly(bean, varName);
        if (property != null) return property;

        String propertyName = EqlUtils.convertUnderscoreNameToPropertyName(varName);
        if (!Objects.equal(propertyName, varName))
            property = EqlUtils.getPropertyQuietly(bean, propertyName);

        return property;
    }
}
