import os
import json
import shutil
from typing import Any, Dict, List, Optional


def _merge_respostas(
    a: Optional[Dict[str, List[str]]], b: Optional[Dict[str, List[str]]]
) -> Optional[Dict[str, List[str]]]:
    """Return union of two respostas dictionaries.

    Each key maps to a list whose first element is the answer value and any
    subsequent elements are respondent names. Duplicates are avoided while
    preserving order and ensuring the answer value stays at position 0.
    """

    if not b:
        return a
    if not a:
        return b

    merged: Dict[str, List[str]] = {k: list(v) for k, v in a.items()}
    for papel, valores in b.items():
        if not isinstance(valores, list) or not valores:
            continue
        existing = merged.setdefault(papel, [])
        if not existing:
            merged[papel] = list(valores)
            continue

        # ensure answer value at index 0
        answer = valores[0]
        if existing:
            if existing[0] != answer and answer not in existing:
                existing.insert(0, answer)
        else:
            existing.append(answer)
        for nome in valores[1:]:
            if nome not in existing[1:]:
                existing.append(nome)
    return merged


def _canonicalize_suprimento_roles(
    respostas: Optional[Dict[str, List[str]]]
) -> Optional[Dict[str, List[str]]]:
    """Ensure suprimento answers aren't stranded under produção aliases.

    Older AppEstoque payloads reused the ``producao`` role for the questions
    that belong to the suprimento block (1.15 a 1.19). The collector already
    tries to remap these aliases, but defensive code here guarantees we never
    emit a merged checklist without the ``suprimento`` key when the supply
    side did answer the question.
    """

    if not isinstance(respostas, dict):
        return respostas
    if "suprimento" in respostas:
        return respostas

    for alias in ("produção", "producao"):
        valores = respostas.get(alias)
        if isinstance(valores, list) and valores:
            ajustado = {
                chave: list(valor) if isinstance(valor, list) else valor
                for chave, valor in respostas.items()
            }
            ajustado.pop(alias, None)
            ajustado["suprimento"] = list(valores)
            return ajustado

    return respostas


def merge_checklists(json_suprimento: Dict[str, Any], json_producao: Dict[str, Any]) -> Dict[str, Any]:
    """Merge two checklist JSON structures according to business rules."""
    obra_sup = json_suprimento.get("obra", "")
    obra_prod = json_producao.get("obra", "")
    ano_sup = json_suprimento.get("ano", "")
    ano_prod = json_producao.get("ano", "")

    obra = obra_sup or obra_prod
    ano = ano_sup or ano_prod

    avisos: List[str] = []
    if obra_sup and obra_prod and obra_sup != obra_prod:
        avisos.append(f"Divergência de obra: suprimento={obra_sup}, produção={obra_prod}")
    if ano_sup and ano_prod and ano_sup != ano_prod:
        avisos.append(f"Divergência de ano: suprimento={ano_sup}, produção={ano_prod}")

    def _first_key(data: Dict[str, Any], keys: List[str]) -> Any:
        for key in keys:
            if key in data:
                return data.get(key)
        return None

    result: Dict[str, Any] = {
        "obra": obra,
        "ano": ano,
        "respondentes": {
            "suprimento": _first_key(json_suprimento, ["suprimento", "produção", "producao"]),
            "produção": _first_key(json_producao, ["montador", "produção", "producao", "suprimento"]),
        },
    }

    buckets: Dict[str, Dict[str, Any]] = {}

    def _bucket_for(pergunta: str) -> Dict[str, Any]:
        bucket = buckets.setdefault(
            pergunta,
            {
                "numeros": set(),
                "pergunta_sup": "",
                "pergunta_prod": "",
                "pergunta_final": pergunta,
                "res_sup": None,
                "res_prod": None,
            },
        )
        if pergunta and len(pergunta) > len(bucket.get("pergunta_final", "")):
            bucket["pergunta_final"] = pergunta
        return bucket

    def _update_pergunta(bucket: Dict[str, Any], chave: str, texto: str) -> None:
        if texto and len(texto) > len(bucket.get(chave, "")):
            bucket[chave] = texto
            if len(texto) > len(bucket.get("pergunta_final", "")):
                bucket["pergunta_final"] = texto

    def _merge_dict(
        a: Optional[Dict[str, List[str]]], b: Optional[Dict[str, List[str]]]
    ) -> Optional[Dict[str, List[str]]]:
        return _merge_respostas(a, b)

    def _extract_respostas(
        item: Dict[str, Any],
        keys: List[str],
        parent: Dict[str, Any],
        *,
        canonical_role: Optional[str] = None,
        alias_roles: Optional[List[str]] = None,
    ) -> Optional[Dict[str, List[str]]]:
        """Collect respostas keyed by role with names appended."""

        coletadas: Dict[str, List[str]] = {}
        respostas = item.get("respostas")
        alias_set = set(alias_roles or [])
        if canonical_role:
            alias_set.add(canonical_role)

        def _store(target: str, valores: List[str]) -> None:
            if target in coletadas:
                merged = _merge_respostas({target: coletadas[target]}, {target: valores})
                coletadas[target] = merged.get(target, valores)
            else:
                coletadas[target] = valores

        if isinstance(respostas, dict):
            for papel in keys:
                valores = respostas.get(papel)
                if isinstance(valores, list):
                    lista = list(valores)
                    target = (
                        canonical_role
                        if canonical_role and papel in alias_set
                        else papel
                    )
                    nome = item.get(papel) or parent.get(papel)
                    if canonical_role and target == canonical_role and not nome:
                        nome = item.get(canonical_role) or parent.get(canonical_role)
                    if nome and nome not in lista[1:]:
                        if lista:
                            lista.append(nome)
                        else:
                            lista = [nome]
                    _store(target, lista)
        elif isinstance(respostas, list):
            for resp in respostas:
                if (
                    isinstance(resp, dict)
                    and "valor" in resp
                    and resp.get("papel")
                ):
                    papel = resp["papel"]
                    target = (
                        canonical_role
                        if canonical_role and papel in alias_set
                        else papel
                    )
                    lista = [resp["valor"]]
                    nome = resp.get("nome")
                    if nome:
                        lista.append(nome)
                    _store(target, lista)

        if not coletadas:
            resposta = item.get("resposta")
            if isinstance(resposta, list):
                papel = keys[0] if keys else "resposta"
                target = (
                    canonical_role
                    if canonical_role and papel in alias_set
                    else papel
                )
                lista = list(resposta)
                nome = parent.get(papel)
                if canonical_role and target == canonical_role and not nome:
                    nome = parent.get(canonical_role)
                if nome:
                    if lista:
                        lista.append(nome)
                    else:
                        lista = [nome]
                coletadas[target] = lista
        return coletadas or None

    for item in json_suprimento.get("itens", []):
        numero = item.get("numero")
        if numero is None:
            continue
        pergunta = item.get("pergunta", "")
        resposta = _extract_respostas(
            item,
            ["suprimento", "produção", "producao"],
            json_suprimento,
            canonical_role="suprimento",
            alias_roles=["suprimento", "produção", "producao"],
        )
        resposta = _canonicalize_suprimento_roles(resposta)
        bucket = _bucket_for(pergunta)
        if numero is not None:
            bucket["numeros"].add(numero)
        _update_pergunta(bucket, "pergunta_sup", pergunta)
        bucket["res_sup"] = _merge_dict(bucket.get("res_sup"), resposta)
    for item in json_producao.get("itens", []):
        numero = item.get("numero")
        if numero is None:
            continue
        pergunta = item.get("pergunta", "")
        resposta = _extract_respostas(
            item,
            ["montador", "produção", "producao", "suprimento"],
            json_producao,
        )
        bucket = _bucket_for(pergunta)
        if numero is not None:
            bucket["numeros"].add(numero)
        _update_pergunta(bucket, "pergunta_prod", pergunta)
        bucket["res_prod"] = _merge_dict(bucket.get("res_prod"), resposta)
        
        
    result_items: List[Dict[str, Any]] = []
    for pergunta, dados in buckets.items():
        numeros = sorted(dados.get("numeros", set()))
        res_sup = dados.get("res_sup")
        res_prod = dados.get("res_prod")
        res_unificado = _merge_dict(res_sup, res_prod)
        pergunta_final = dados.get("pergunta_final") or pergunta
        result_items.append(
            {
                "numero": numeros,
                "pergunta": pergunta_final,
                "respostas": res_unificado,
            }
        )
    result["itens"] = sorted(
        result_items, key=lambda x: x["numero"][0] if x.get("numero") else 0
    )

    def _first_from_items(data: Dict[str, Any], roles: List[str]) -> Optional[str]:
        for item in data.get("itens", []):
            for role in roles:
                nome = item.get(role)
                if isinstance(nome, str) and nome.strip():
                    return nome.strip()
            respostas = item.get("respostas")
            if isinstance(respostas, dict):
                for role in roles:
                    valores = respostas.get(role)
                    if isinstance(valores, list):
                        for candidato in valores[1:]:
                            if isinstance(candidato, str) and candidato.strip():
                                return candidato.strip()
            elif isinstance(respostas, list):
                for resp in respostas:
                    if (
                        isinstance(resp, dict)
                        and resp.get("papel") in roles
                        and isinstance(resp.get("nome"), str)
                        and resp["nome"].strip()
                    ):
                        return resp["nome"].strip()
        return None

    if not result["respondentes"].get("suprimento"):
        nome = _first_from_items(json_suprimento, ["suprimento", "produção", "producao"])
        if nome:
            result["respondentes"]["suprimento"] = nome

    if not result["respondentes"].get("produção"):
        nome = _first_from_items(
            json_producao, ["montador", "produção", "producao", "suprimento"]
        )
        if nome:
            result["respondentes"]["produção"] = nome

    materiais: Dict[str, Dict[str, Any]] = {}
    for mat in json_suprimento.get("materiais", []) + json_producao.get("materiais", []):
        nome = mat.get("material")
        if not nome:
            continue
        quantidade = mat.get("quantidade", 0) or 0
        completo = bool(mat.get("completo"))
        registro = materiais.setdefault(nome, {"material": nome, "quantidade": 0, "completo": True})
        registro["quantidade"] += quantidade
        registro["completo"] = registro["completo"] and completo
    result["materiais"] = list(materiais.values()) if materiais else []

    if avisos:
        result["avisos"] = avisos

    return result


def _dedup_items(items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Deduplicate checklist items grouping by ``pergunta`` when available."""

    merged: Dict[Any, Dict[str, Any]] = {}
    for item in items:
        key = item.get("pergunta") or item.get("numero")
        if key is None:
            continue

        if key not in merged:
            numero = item.get("numero")
            if isinstance(numero, list):
                numeros = list(numero)
            elif numero is None:
                numeros = []
            else:
                numeros = [numero]
            merged[key] = {
                "numero": numeros,
                "pergunta": item.get("pergunta", ""),
                "respostas": {},
            }

        existing = merged[key]

        numero_novo = item.get("numero")
        if isinstance(numero_novo, list):
            for n in numero_novo:
                if n not in existing["numero"]:
                    existing["numero"].append(n)
        elif numero_novo is not None and numero_novo not in existing["numero"]:
            existing["numero"].append(numero_novo)

        resp_obj: Optional[Dict[str, List[str]]] = None
        if isinstance(item.get("respostas"), dict):
            resp_obj = item.get("respostas")
        elif isinstance(item.get("respostas"), list):
            temp: Dict[str, List[str]] = {}
            for resp in item.get("respostas") or []:
                if isinstance(resp, dict) and "valor" in resp:
                    papel = resp.get("papel") or "resposta"
                    lista = temp.setdefault(papel, [])
                    lista.append(resp["valor"])
                    nome = resp.get("nome")
                    if nome:
                        lista.append(nome)
            resp_obj = temp
        elif isinstance(item.get("resposta"), list):
            resp_obj = {"resposta": list(item.get("resposta") or [])}

        existing["respostas"] = _merge_respostas(existing.get("respostas"), resp_obj)

        if len(item.get("pergunta", "")) > len(existing.get("pergunta", "")):
            existing["pergunta"] = item["pergunta"]

    try:
        return sorted(
            merged.values(), key=lambda x: x["numero"][0] if x.get("numero") else 0
        )
    except Exception:
        return list(merged.values())



def merge_directory(base_dir: str, output_dir: Optional[str] = None) -> List[Dict[str, Any]]:
    """Merge checklist pairs found in ``base_dir`` grouped by ``obra``.

    ``output_dir`` defaults to ``base_dir/Posto01_Oficina``.
    The original JSON files used in the merge are removed from ``base_dir``.
    Returns a list of merged checklist objects.
    """
    files = [f for f in os.listdir(base_dir) if f.endswith(".json")]
    by_obra: Dict[str, List[Dict[str, Any]]] = {}
    for fname in files:
        path = os.path.join(base_dir, fname)
        try:
            with open(path, "r", encoding="utf-8") as fp:
                data = json.load(fp)
        except Exception:
            continue
        obra = data.get("obra")
        if not obra:
            continue
        by_obra.setdefault(obra, []).append({"data": data, "path": path})
    output_dir = output_dir or os.path.join(base_dir, "Posto01_Oficina")
    os.makedirs(output_dir, exist_ok=True)
    merged: List[Dict[str, Any]] = []
    for obra, entries in by_obra.items():
        # group entries by type and select the most recent one of each
        sup_entries = [
            e
            for e in entries
            if "suprimento" in e["data"]
            and str(e["data"].get("origem", "")).strip().lower() != "appoficina"
        ]

        def _is_production(entry: Dict[str, Any]) -> bool:
            data = entry["data"]
            origem = str(data.get("origem", "")).strip().lower()
            if origem == "appoficina":
                return True
            if any(k in data for k in ("produção", "producao", "montador")):
                return True
            for item in data.get("itens", []) or []:
                respostas = item.get("respostas") or {}
                if any(k in respostas for k in ("montador", "produção", "producao")):
                    return True
            return False

        prod_entries = [e for e in entries if _is_production(e)]
        sup = (
            max(sup_entries, key=lambda e: os.path.getmtime(e["path"]))
            if sup_entries
            else None
        )
        prod = (
            max(prod_entries, key=lambda e: os.path.getmtime(e["path"]))
            if prod_entries
            else None
        )

        # remove older checklist files to avoid leftovers
        for entry in sup_entries:
            if sup is None or entry["path"] != sup["path"]:
                try:
                    os.remove(entry["path"])
                except OSError:
                    pass
        for entry in prod_entries:
            if prod is None or entry["path"] != prod["path"]:
                try:
                    os.remove(entry["path"])
                except OSError:
                    pass

        if not (sup and prod):
            continue

        result = merge_checklists(sup["data"], prod["data"])
        out_path = os.path.join(output_dir, f"checklist_{obra}.json")

        # merge with existing checklist if present, overwriting only updated items
        if os.path.exists(out_path):
            try:
                with open(out_path, "r", encoding="utf-8") as fp:
                    existing = json.load(fp)
            except Exception:
                existing = {}

            combined_items = existing.get("itens", []) + result.get("itens", [])
            result["itens"] = _dedup_items(combined_items)

            existing_mats = {
                m.get("material"): m
                for m in existing.get("materiais", [])
                if m.get("material")
            }
            for mat in result.get("materiais", []) or []:
                nome = mat.get("material")
                if nome:
                    existing_mats[nome] = mat
            if existing_mats:
                result["materiais"] = list(existing_mats.values())

        with open(out_path, "w", encoding="utf-8") as fp:
            json.dump(result, fp, ensure_ascii=False, indent=2)
        try:
            os.remove(sup["path"])
            os.remove(prod["path"])
        except OSError:
            pass
        merged.append(result)
    return merged


def find_mismatches(directory: str) -> List[Dict[str, Any]]:
    """Return merged checklists that have missing answers.

    Looks for ``checklist_*.json`` files inside ``directory`` and checks each
    item where the unified ``respostas`` list is missing or empty. Only
    checklists containing at least one such item are returned.
    """

    resultados: List[Dict[str, Any]] = []
    files = [f for f in os.listdir(directory) if f.startswith("checklist_") and f.endswith(".json")]
    for fname in files:
        path = os.path.join(directory, fname)
        try:
            with open(path, "r", encoding="utf-8") as fp:
                data = json.load(fp)
        except Exception:
            continue

        divergencias: List[Dict[str, Any]] = []
        for item in data.get("itens", []):
            resp = item.get("respostas")
            has_resposta = False
            if isinstance(resp, dict):
                for valores in resp.values():
                    if isinstance(valores, list) and valores:
                        has_resposta = True
                        break
            elif isinstance(resp, list):
                has_resposta = bool(resp)
            elif isinstance(item.get("resposta"), list):  # compat
                resp = item.get("resposta")
                has_resposta = bool(resp)
            if not has_resposta:
                divergencias.append(
                    {
                        "numero": item.get("numero"),
                        "pergunta": item.get("pergunta"),
                        "respostas": resp,
                    }
                )
        if divergencias:
            resultados.append(
                {
                    "obra": data.get("obra", ""),
                    "ano": data.get("ano", ""),
                    "divergencias": divergencias,
                }
            )

    return resultados


def move_matching_checklists(base_dir: str) -> List[str]:
    """Move merged checklists with complete answers to the next stage.

    Looks into ``Posto01_Oficina`` inside ``base_dir`` and moves any
    ``checklist_*.json`` files where all items contain a non-empty ``respostas``
    list to ``Posto02_Oficina``. Returns a list of moved filenames.
    """

    src_dir = os.path.join(base_dir, "Posto01_Oficina")
    if not os.path.isdir(src_dir):
        return []
    dest_dir = os.path.join(base_dir, "Posto02_Oficina")
    os.makedirs(dest_dir, exist_ok=True)

    # determine which obras still have divergences
    divergentes = {entry["obra"] for entry in find_mismatches(src_dir)}

    moved: List[str] = []
    for fname in os.listdir(src_dir):
        if not (fname.startswith("checklist_") and fname.endswith(".json")):
            continue
        path = os.path.join(src_dir, fname)
        try:
            with open(path, "r", encoding="utf-8") as fp:
                data = json.load(fp)
        except Exception:
            continue
        obra = data.get("obra", "")
        if obra in divergentes:
            continue
        shutil.move(path, os.path.join(dest_dir, fname))
        moved.append(fname)
    return moved


def main() -> None:
    """Command-line interface for merging checklist JSON files."""
    import argparse

    parser = argparse.ArgumentParser(
        description="Merge suprimento and produção checklists found in a directory"
    )
    parser.add_argument(
        "base_dir",
        nargs="?",
        default=".",
        help="Directory containing checklist JSON files",
    )
    parser.add_argument(
        "-o",
        "--output",
        dest="output_dir",
        default=None,
        help="Directory to write merged checklists",
    )
    args = parser.parse_args()
    merged = merge_directory(args.base_dir, args.output_dir)
    moved = move_matching_checklists(args.base_dir)
    print(f"Merged {len(merged)} checklist(s)")
    if moved:
        print(f"Moved {len(moved)} checklist(s) to Posto02_Oficina")


if __name__ == "__main__":
    main()
