package mil.nga.giat.mage.map.preference;


import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Comparator;

import mil.nga.giat.mage.database.model.layer.Layer;

public class LayerNameComparator implements Comparator<Layer> {
    @Override
    public int compare(Layer o1, Layer o2) {
        return new CompareToBuilder().append(o1.getName(), o2.getName()).toComparison();
    }
}
