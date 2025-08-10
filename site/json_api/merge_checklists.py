import os
import json
from typing import Any, Dict, List, Optional


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

    result: Dict[str, Any] = {
        "obra": obra,
        "ano": ano,
        "respondentes": {
            "suprimento": json_suprimento.get("suprimento"),
            "produção": json_producao.get("produção"),
        },
    }

    itens: Dict[int, Dict[str, Any]] = {}
    for item in json_suprimento.get("itens", []):
        numero = item.get("numero")
        if numero is None:
            continue
        pergunta = item.get("pergunta", "")
        resposta = item.get("resposta")
        itens.setdefault(numero, {})["pergunta_sup"] = pergunta
        itens[numero]["res_sup"] = resposta if isinstance(resposta, list) else None
    for item in json_producao.get("itens", []):
        numero = item.get("numero")
        if numero is None:
            continue
        pergunta = item.get("pergunta", "")
        resposta = item.get("resposta")
        entry = itens.setdefault(numero, {})
        entry["pergunta_prod"] = pergunta
        entry["res_prod"] = resposta if isinstance(resposta, list) else None

    result_items: List[Dict[str, Any]] = []
    for numero in sorted(itens):
        entry = itens[numero]
        pergunta_sup = entry.get("pergunta_sup", "")
        pergunta_prod = entry.get("pergunta_prod", "")
        pergunta = pergunta_sup if len(pergunta_sup) >= len(pergunta_prod) else pergunta_prod
        result_items.append(
            {
                "numero": numero,
                "pergunta": pergunta or pergunta_prod or pergunta_sup,
                "respostas": {
                    "suprimento": entry.get("res_sup"),
                    "produção": entry.get("res_prod"),
                },
            }
        )
    result["itens"] = result_items

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
        sup = next((e for e in entries if "suprimento" in e["data"]), None)
        prod = next((e for e in entries if "produção" in e["data"]), None)
        if not (sup and prod):
            continue
        result = merge_checklists(sup["data"], prod["data"])
        out_path = os.path.join(output_dir, f"checklist_{obra}.json")
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
    """Return merged checklists that have differing answers.

    Looks for ``checklist_*.json`` files inside ``directory`` and checks each
    item where both ``suprimento`` and ``produção`` answers are present but
    differ. Only checklists containing at least one divergence are returned.
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
            resp = item.get("respostas", {})
            sup = resp.get("suprimento")
            prod = resp.get("produção")
            if sup is not None and prod is not None and sup != prod:
                divergencias.append(
                    {
                        "numero": item.get("numero"),
                        "pergunta": item.get("pergunta"),
                        "suprimento": sup,
                        "produção": prod,
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
    print(f"Merged {len(merged)} checklist(s)")


if __name__ == "__main__":
    main()
