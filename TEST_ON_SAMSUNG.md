# Test Samsung — Mon équilibre Sync 0.2.1

## Objectif du premier test

Valider uniquement la chaîne réelle :

Samsung Health → Health Connect → Mon équilibre Sync → compte Supabase Mon équilibre.

## Avant l'installation

Dans Samsung Health, vérifiez que la synchronisation vers Health Connect est activée pour :
- pas ;
- exercice ;
- distance.

## Dans l'application

1. Ouvrez **Mon équilibre Sync**.
2. Connectez-vous avec le même e-mail/mot de passe que Mon équilibre.
3. Appuyez sur **Autoriser Health Connect**.
4. Accordez uniquement les trois autorisations demandées : pas, exercices et distance.
5. Vérifiez le nombre de pas affiché.
6. Vérifiez qu'une séance Samsung Health enregistrée aujourd'hui apparaît, avec sa durée et sa distance si Samsung l'a transmise.
7. Appuyez sur **Synchroniser maintenant**.
8. Le message attendu est : `Synchronisé : X pas et Y séance(s).`

## Important

- La synchronisation est manuelle dans cette version.
- Aucune valeur de `daily_entries` n'est écrasée.
- Les données Health Connect vont dans des tables séparées et protégées par RLS.
- Si la distance affiche `Distance non fournie`, cela peut venir de Samsung Health/Health Connect et ne signifie pas forcément que le connecteur est cassé.

## En cas d'erreur

Conservez le texte exact affiché à l'écran. Ne réinstallez pas et ne changez pas les permissions au hasard : l'erreur précise permettra de corriger la prochaine étape.
