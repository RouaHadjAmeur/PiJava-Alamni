-- Table pour les conversations
CREATE TABLE IF NOT EXISTS `conversation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sujet` varchar(255) NOT NULL,
  `contenu` text NOT NULL,
  `date_creation` date NOT NULL,
  `expediteur_email` varchar(255) NOT NULL,
  `destinataire_email` varchar(255) NOT NULL,
  `statut` varchar(50) NOT NULL DEFAULT 'Non lu',
  `expediteur_id` int(11) NOT NULL,
  `destinataire_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ajout de quelques données de test
INSERT INTO `conversation` (`sujet`, `contenu`, `date_creation`, `expediteur_email`, `destinataire_email`, `statut`, `expediteur_id`, `destinataire_id`) VALUES
('Question sur le cours de mathématiques', 'Bonjour, j\'aurais besoin de précisions concernant le dernier chapitre sur les intégrales.', '2024-11-15', 'eleve@example.com', 'prof@example.com', 'Non lu', 1, 2),
('Absence prévue', 'Bonjour, je vous informe que mon enfant sera absent la semaine prochaine pour raisons médicales.', '2024-11-16', 'parent@example.com', 'prof@example.com', 'Lu', 3, 2),
('Réunion parents-professeurs', 'Bonjour, je vous confirme la date de la prochaine réunion parents-professeurs pour le 25 novembre.', '2024-11-17', 'admin@example.com', 'parent@example.com', 'Non lu', 4, 3); 