
package org.bgi.flexlab.gaea.tools.genotyer.annotator;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cern.jet.math.Arithmetic;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import org.bgi.flexlab.gaea.data.structure.alignment.AlignmentsBasic;
import org.bgi.flexlab.gaea.data.structure.pileup.Mpileup;
import org.bgi.flexlab.gaea.data.structure.pileup.Pileup;
import org.bgi.flexlab.gaea.data.structure.pileup.PileupReadInfo;
import org.bgi.flexlab.gaea.data.structure.reference.ChromosomeInformationShare;
import org.bgi.flexlab.gaea.data.structure.vcf.VariantDataTracker;
import org.bgi.flexlab.gaea.tools.genotyer.GenotypeLikelihoodCalculator.PerReadAlleleLikelihoodMap;
import org.bgi.flexlab.gaea.tools.genotyer.annotator.interfaces.ActiveRegionBasedAnnotation;
import org.bgi.flexlab.gaea.tools.genotyer.annotator.interfaces.InfoFieldAnnotation;
import org.bgi.flexlab.gaea.tools.genotyer.annotator.interfaces.StandardAnnotation;
import org.bgi.flexlab.gaea.util.QualityUtils;


/**
 * Phred-scaled p-value using Fisher's Exact Test to detect strand bias (the variation
 * being seen on only the forward or only the reverse strand) in the reads? More bias is
 * indicative of false positive calls.  Note that the fisher strand test may not be
 * calculated for certain complex indel cases or for multi-allelic sites.
 */
public class FisherStrand extends InfoFieldAnnotation implements StandardAnnotation, ActiveRegionBasedAnnotation {
    private static final String FS = "FS";
    private static final double MIN_PVALUE = 1E-320;
    private static final int MIN_QUAL_FOR_FILTERED_TEST = 17;

    public Map<String, Object> annotate(final VariantDataTracker tracker,
                                        final ChromosomeInformationShare ref,
                                        final Mpileup mpileup,
                                        final VariantContext vc,
                                        final Map<String, PerReadAlleleLikelihoodMap> stratifiedPerReadAlleleLikelihoodMap) {
        if ( !vc.isVariant() )
            return null;

        if (vc.isSNP() && mpileup != null) {
            final int[][] tableNoFiltering = getSNPContingencyTable(mpileup, vc.getReference(), vc.getAltAlleleWithHighestAlleleCount(), -1);
            final int[][] tableFiltering = getSNPContingencyTable(mpileup, vc.getReference(), vc.getAltAlleleWithHighestAlleleCount(), MIN_QUAL_FOR_FILTERED_TEST);
            return pValueForBestTable(tableFiltering, tableNoFiltering);
        }
        else if (stratifiedPerReadAlleleLikelihoodMap != null) {
            // either SNP with no alignment context, or indels: per-read likelihood map needed
            final int[][] table = getContingencyTable(stratifiedPerReadAlleleLikelihoodMap, vc);
            return pValueForBestTable(table, null);
        }
        else
            // for non-snp variants, we  need per-read likelihoods.
            // for snps, we can get same result from simple pileup
            return null;
    }

    /**
     * Create an annotation for the highest (i.e., least significant) p-value of table1 and table2
     *
     * @param table1 a contingency table, may be null
     * @param table2 a contingency table, may be null
     * @return annotation result for FS given tables
     */
    private Map<String, Object> pValueForBestTable(final int[][] table1, final int[][] table2) {
        if ( table2 == null )
            return table1 == null ? null : annotationForOneTable(pValueForContingencyTable(table1));
        else if (table1 == null)
            return annotationForOneTable(pValueForContingencyTable(table2));
        else { // take the one with the best (i.e., least significant pvalue)
            double pvalue1 = Math.max(pValueForContingencyTable(table1), MIN_PVALUE);
            double pvalue2 = Math.max(pValueForContingencyTable(table2), MIN_PVALUE);
            return annotationForOneTable(Math.max(pvalue1, pvalue2));
        }
    }

    /**
     * Returns an annotation result given a pValue
     *
     * @param pValue
     * @return a hash map from FS -> phred-scaled pValue
     */
    private Map<String, Object> annotationForOneTable(final double pValue) {
        final Object value = String.format("%.3f", QualityUtils.phredScaleErrorRate(pValue));
        return Collections.singletonMap(FS, value);
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put(FS, String.format("%.3f", QualityUtils.phredScaleErrorRate(pValue)));
//        return map;
    }

    public List<String> getKeyNames() {
        return Arrays.asList(FS);
    }

    public List<VCFInfoHeaderLine> getDescriptions() {
        return Arrays.asList(
            new VCFInfoHeaderLine(FS, 1, VCFHeaderLineType.Float, "Phred-scaled p-value using Fisher's exact test to detect strand bias"));
    }

    private Double pValueForContingencyTable(int[][] originalTable) {
        int [][] table = copyContingencyTable(originalTable);

        double pCutoff = computePValue(table);
        //printTable(table, pCutoff);

        double pValue = pCutoff;
        while (rotateTable(table)) {
            double pValuePiece = computePValue(table);

            //printTable(table, pValuePiece);

            if (pValuePiece <= pCutoff) {
                pValue += pValuePiece;
            }
        }

        table = copyContingencyTable(originalTable);
        while (unrotateTable(table)) {
            double pValuePiece = computePValue(table);

            //printTable(table, pValuePiece);

            if (pValuePiece <= pCutoff) {
                pValue += pValuePiece;
            }
        }

        //System.out.printf("P-cutoff: %f\n", pCutoff);
        //System.out.printf("P-value: %f\n\n", pValue);

       return pValue;
    }

    private static int [][] copyContingencyTable(int [][] t) {
        int[][] c = new int[2][2];

        for ( int i = 0; i < 2; i++ )
            for ( int j = 0; j < 2; j++ )
                c[i][j] = t[i][j];

        return c;
    }


    private static void printTable(int[][] table, double pValue) {
        System.out.printf("%d %d; %d %d : %f\n", table[0][0], table[0][1], table[1][0], table[1][1], pValue);
    }

    private static boolean rotateTable(int[][] table) {
        table[0][0] -= 1;
        table[1][0] += 1;

        table[0][1] += 1;
        table[1][1] -= 1;

        return (table[0][0] >= 0 && table[1][1] >= 0);
    }

    private static boolean unrotateTable(int[][] table) {
        table[0][0] += 1;
        table[1][0] -= 1;

        table[0][1] -= 1;
        table[1][1] += 1;

        return (table[0][1] >= 0 && table[1][0] >= 0);
    }

    private static double computePValue(int[][] table) {

        int[] rowSums = { sumRow(table, 0), sumRow(table, 1) };
        int[] colSums = { sumColumn(table, 0), sumColumn(table, 1) };
        int N = rowSums[0] + rowSums[1];

        // calculate in log space so we don't die with high numbers
        double pCutoff = Arithmetic.logFactorial(rowSums[0])
                         + Arithmetic.logFactorial(rowSums[1])
                         + Arithmetic.logFactorial(colSums[0])
                         + Arithmetic.logFactorial(colSums[1])
                         - Arithmetic.logFactorial(table[0][0])
                         - Arithmetic.logFactorial(table[0][1])
                         - Arithmetic.logFactorial(table[1][0])
                         - Arithmetic.logFactorial(table[1][1])
                         - Arithmetic.logFactorial(N);
        return Math.exp(pCutoff);
    }

    private static int sumRow(int[][] table, int column) {
        int sum = 0;
        for (int r = 0; r < table.length; r++) {
            sum += table[r][column];
        }

        return sum;
    }

    private static int sumColumn(int[][] table, int row) {
        int sum = 0;
        for (int c = 0; c < table[row].length; c++) {
            sum += table[row][c];
        }

        return sum;
    }

    /**
     Allocate and fill a 2x2 strand contingency table.  In the end, it'll look something like this:
     *             fw      rc
     *   allele1   #       #
     *   allele2   #       #
     * @return a 2x2 contingency table
     */
    private static int[][] getContingencyTable( final Map<String, PerReadAlleleLikelihoodMap> stratifiedPerReadAlleleLikelihoodMap, final VariantContext vc) {
        final Allele ref = vc.getReference();
        final Allele alt = vc.getAltAlleleWithHighestAlleleCount();
        int[][] table = new int[2][2];

        for (PerReadAlleleLikelihoodMap maps : stratifiedPerReadAlleleLikelihoodMap.values() ) {
            for (Map.Entry<AlignmentsBasic ,Map<Allele,Double>> el : maps.getLikelihoodReadMap().entrySet()) {
                final boolean matchesRef = PerReadAlleleLikelihoodMap.getMostLikelyAllele(el.getValue()).equals(ref,true);
                final boolean matchesAlt = PerReadAlleleLikelihoodMap.getMostLikelyAllele(el.getValue()).equals(alt,true);

                if ( !matchesRef && !matchesAlt )
                    continue;

                boolean isFW = el.getKey().isReverse();

                int row = matchesRef ? 0 : 1;
                int column = isFW ? 0 : 1;

                final AlignmentsBasic read = el.getKey();
                table[row][column] += 1;
            }
        }

        return table;
    }
    /**
     Allocate and fill a 2x2 strand contingency table.  In the end, it'll look something like this:
     *             fw      rc
     *   allele1   #       #
     *   allele2   #       #
     * @return a 2x2 contingency table
     */
    private static int[][] getSNPContingencyTable(final Mpileup mpileup,
                                                  final Allele ref,
                                                  final Allele alt,
                                                  final int minQScoreToConsider ) {
        int[][] table = new int[2][2];

        for ( String sample : mpileup.getCurrentPosPileup().keySet() ) {
            Pileup pileup = mpileup.getCurrentPosPileup().get(sample);
            for (PileupReadInfo p : pileup.getPlp()) {

                // ignore reduced reads because they are always on the forward strand!
                // TODO -- when het compression is enabled in RR, we somehow need to allow those reads through into the Fisher test
                //if ( p.getRead().isReducedRead() )
               //     continue;

                if ( ! RankSumTest.isUsableBase(p, false) ) // ignore deletions
                    continue;

                if ( p.getBaseQuality() < minQScoreToConsider || p.getMappingQuality() < minQScoreToConsider )
                    continue;

                final Allele base = Allele.create((byte)p.getBase(), false);
                final boolean isFW = !p.getReadInfo().isReverse();

                final boolean matchesRef = ref.equals(base, true);
                final boolean matchesAlt = alt.equals(base, true);
                if ( matchesRef || matchesAlt ) {
                    int row = matchesRef ? 0 : 1;
                    int column = isFW ? 0 : 1;

                    table[row][column] += 1;
                }
            }
        }

        return table;
    }
}
