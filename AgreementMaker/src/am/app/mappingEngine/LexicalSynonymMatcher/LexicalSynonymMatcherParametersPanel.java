package am.app.mappingEngine.LexicalSynonymMatcher;

import java.util.prefs.Preferences;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;

import am.app.Core;
import am.app.lexicon.Lexicon;
import am.app.lexicon.subconcept.SubconceptSynonymLexicon;
import am.app.mappingEngine.AbstractMatcherParametersPanel;
import am.app.mappingEngine.AbstractParameters;
import am.app.mappingEngine.LexiconStore.LexiconRegistry;
import am.app.ontology.Ontology;

public class LexicalSynonymMatcherParametersPanel extends AbstractMatcherParametersPanel {

	private static final long serialVersionUID = 772347863431325953L;

	private JCheckBox chkUseSubconceptSynonyms;
	private static final String PREF_USE_SYNONYM_TERMS = "PREF_USE_SYNONYM_TERMS";
	
	public LexicalSynonymMatcherParametersPanel() {
		super();
		
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		
		chkUseSubconceptSynonyms = new JCheckBox("Use synonym terms.");
		chkUseSubconceptSynonyms.setEnabled(false);
		
		Ontology sourceOntology = Core.getInstance().getSourceOntology();
		Ontology targetOntology = Core.getInstance().getTargetOntology();
		
		try {
			Lexicon sourceOntLexicon = Core.getLexiconStore().getLexicon(sourceOntology.getID(), LexiconRegistry.ONTOLOGY_LEXICON);
			Lexicon targetOntLexicon = Core.getLexiconStore().getLexicon(targetOntology.getID(), LexiconRegistry.ONTOLOGY_LEXICON);
			
			if( sourceOntLexicon instanceof SubconceptSynonymLexicon || 
				targetOntLexicon instanceof SubconceptSynonymLexicon ){
				
				chkUseSubconceptSynonyms.setEnabled(true);
				chkUseSubconceptSynonyms.setSelected( prefs.getBoolean(PREF_USE_SYNONYM_TERMS, false) );
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		
		
		GroupLayout layout = new GroupLayout(this);
		
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup( layout.createParallelGroup() 
				.addComponent(chkUseSubconceptSynonyms)
		);
		
		layout.setVerticalGroup( layout.createSequentialGroup() 
				.addComponent(chkUseSubconceptSynonyms)
		);
	}

	@Override
	public AbstractParameters getParameters() {
		
		LexicalSynonymMatcherParameters params = new LexicalSynonymMatcherParameters();
		
		params.useSynonymTerms = chkUseSubconceptSynonyms.isSelected();
		
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		prefs.putBoolean(PREF_USE_SYNONYM_TERMS, params.useSynonymTerms);
		
		return params;
	}
}
