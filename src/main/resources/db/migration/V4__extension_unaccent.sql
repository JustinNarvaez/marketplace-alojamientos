-- Permite comparar texto sin tildes (unaccent('Bogotá') = 'Bogota'). La
-- usa el buscador de alojamientos para que "medellin" encuentre "Medellín".
CREATE EXTENSION IF NOT EXISTS unaccent;
