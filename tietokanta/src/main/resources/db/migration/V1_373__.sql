INSERT INTO raportti (nimi, kuvaus, konteksti, parametrit, koodi, urakkatyyppi)
VALUES ('toimenpidepaivat','Toimenpidepäivät',
        ARRAY['urakka'::raporttikonteksti, 'hallintayksikko'::raporttikonteksti,
        'hankinta-alue'::raporttikonteksti, 'koko maa'::raporttikonteksti],
        ARRAY[('Aikaväli','aikavali',true,NULL)::raporttiparametri,
        ('Näytä urakka-alueet eriteltynä','urakoittain',true,'koko maa')::raporttiparametri,
        ('Näytä urakka-alueet eriteltynä','urakoittain',true,'hallintayksikko')::raporttiparametri,
        ('Näytä urakka-alueet eriteltynä','urakoittain',true,'hankinta-alue')::raporttiparametri,
        ('Hoitoluokat', 'hoitoluokat', true, NULL)::raporttiparametri],
        '#''harja.palvelin.raportointi.raportit.toimenpidepaivat/suorita',
        'hoito');
