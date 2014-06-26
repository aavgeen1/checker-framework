package org.checkerframework.qualframework.poly;

import java.util.*;

import javax.lang.model.element.Element;

import org.checkerframework.qualframework.base.TypeAnnotator;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.util.ExtendedDeclaredType;
import org.checkerframework.qualframework.util.ExtendedExecutableType;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

/*
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.util.QualifierMapVisitor;
import org.checkerframework.qualframework.util.SetQualifierVisitor;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
*/

/** {@link TypeAnnotator} implementation for qualifier parameter checkers. */
public class QualifierParameterTypeAnnotator<Q> extends TypeAnnotator<QualParams<Q>> {
    private QualifierHierarchy<Wildcard<Q>> containmentHierarchy;

    public QualifierParameterTypeAnnotator(
            QualifierParameterAnnotationConverter<Q> annotationConverter,
            QualifierHierarchy<Wildcard<Q>> containmentHierarchy) {
        super(annotationConverter, new QualParams<>());
        this.containmentHierarchy = containmentHierarchy;
    }

    public QualifierHierarchy<Wildcard<Q>> getContainmentHierarchy() {
        return containmentHierarchy;
    }

    public QualifierParameterAnnotationConverter<Q> getAnnotationConverter() {
        return (QualifierParameterAnnotationConverter<Q>)super.getAnnotationConverter();
    }

    @Override
    protected QualParams<Q> getQualifier(ExtendedTypeMirror type) {
        // Use the appropriate top qualifier by default.  The top qualifier in
        // this system assigns an unbounded wildcard to every parameter.

        QualParams<Q> result = super.getQualifier(type);

        // Find the names of all parameters that are valid on this type.
        Set<String> names = null;

        switch (type.getKind()) {
            case DECLARED:
                names = getAnnotationConverter().getDeclaredParameters(((ExtendedDeclaredType)type).asElement());
                break;
            case EXECUTABLE:
                names = getAnnotationConverter().getDeclaredParameters(((ExtendedExecutableType)type).asElement());
                break;
            case VOID:
            case PACKAGE:
            case NONE:
            case TYPEVAR:
                names = Collections.emptySet();
                break;
            case ARRAY:
                names = Collections.singleton("Main");
                break;
            case INTERSECTION:
            case UNION:
            case NULL:
            case WILDCARD:
                // TODO - figure out the correct behavior for these cases
                names = Collections.emptySet();
                break;
            default:
                // TODO: the checker should get to make this decision.  Maybe
                // take the parameters from the declaration of the boxed
                // version of the primitive type?
                if (type.getKind().isPrimitive()) {
                    names = Collections.singleton("Main");
                    break;
                }
                throw new IllegalArgumentException("unexpected type kind: " + type.getKind());
        }

        if (names.isEmpty()) {
            return result;
        }

        // Add an unbounded wildcard for each parameter that wasn't set.

        Map<String, Wildcard<Q>> newParams = new HashMap<>(result);

        for (String name : names) {
            if (!newParams.containsKey(name)) {
                newParams.put(name, containmentHierarchy.getTop());
            }
        }

        return new QualParams<>(newParams);
    }
}

